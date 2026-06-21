package com.trace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trace.entity.ChatHistory;
import com.trace.entity.LongTermMemory;
import com.trace.mapper.ChatHistoryMapper;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.constant.constant.REDIS_KEY_PREFIX;
import static com.constant.constant.memory.*;

@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ChatHistoryMapper chatHistoryMapper;
    private final LongTermMemoryMapper memoryMapper;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;

    private static final double SIMILARITY_THRESHOLD = 0.85;

    public MemoryServiceImpl(StringRedisTemplate stringRedisTemplate,
                              ChatHistoryMapper chatHistoryMapper,
                              LongTermMemoryMapper memoryMapper,
                              ObjectMapper objectMapper,
                              @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.chatHistoryMapper = chatHistoryMapper;
        this.memoryMapper = memoryMapper;
        this.objectMapper = objectMapper;
        this.embeddingModel = embeddingModel;
    }

    @Override
    @Transactional
    public void saveChatHistory(Long userId, String role, String content) {
        String key = REDIS_KEY_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(Map.of("role", role, "content", content));
            stringRedisTemplate.opsForList().rightPush(key, json);
            Long size = stringRedisTemplate.opsForList().size(key);
            if (size != null && size > MAX_SHORT_TERM)
                stringRedisTemplate.opsForList().trim(key, -MAX_SHORT_TERM, -1);
        } catch (Exception e) {
            log.warn("Failed to save short-term context to Redis: userId={}", userId, e);
        }
        int count = chatHistoryMapper.countByUserId(userId);
        if (count >= MAX_CHAT_HISTORY) {
            int excess = count - MAX_CHAT_HISTORY + 1;
            chatHistoryMapper.deleteOldestByUserId(userId, excess);
        }
        ChatHistory record = ChatHistory.builder().userId(userId).role(role).content(content).build();
        chatHistoryMapper.insert(record);
    }

    @Override
    public List<ChatHistory> getRecentChats(Long userId, int limit) {
        return chatHistoryMapper.findRecentByUserId(userId, limit);
    }

    @Override
    public List<ChatHistory> getChatsBefore(Long userId, Long beforeId, int limit) {
        if (beforeId == null) return getRecentChats(userId, limit);
        return chatHistoryMapper.findBefore(userId, beforeId, limit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getChatContext(Long userId) {
        String key = REDIS_KEY_PREFIX + userId;
        try {
            List<String> entries = stringRedisTemplate.opsForList().range(key, 0, -1);
            if (entries == null || entries.isEmpty()) return List.of();
            List<Map<String, String>> result = new ArrayList<>();
            for (String json : entries) {
                if (json == null || json.isBlank()) continue;
                try {
                    Map<String, String> map = objectMapper.readValue(json,
                            objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
                    result.add(map);
                } catch (Exception e) { log.debug("Skipping unparseable entry"); }
            }
            return result;
        } catch (Exception e) {
            stringRedisTemplate.delete(key);
            return List.of();
        }
    }

    @Override
    @Transactional
    public void saveMemory(Long userId, String content, String sourceType) {
        saveMemory(userId, content, sourceType, null);
    }

    @Override
    @Transactional
    public void saveMemory(Long userId, String content, String sourceType, String embedding) {
        if (content == null || content.isBlank()) return;
        if (embedding != null) {
            List<Map<String, Object>> similar = memoryMapper.findSimilarMemory(userId, embedding, SIMILARITY_THRESHOLD);
            if (similar != null && !similar.isEmpty()) {
                Map<String, Object> match = similar.get(0);
                Long existingId = ((Number) match.get("id")).longValue();
                double sim = ((Number) match.get("similarity")).doubleValue();
                log.info("Memory merged: userId={}, sim={}, oldId={}", userId, String.format("%.3f", sim), existingId);
                memoryMapper.updateContentAndEmbedding(existingId, content, embedding);
                return;
            }
        } else {
            if (isDuplicate(userId, content)) { log.debug("Duplicate skipped"); return; }
        }
        LongTermMemory memory = LongTermMemory.builder().userId(userId).content(content).sourceType(sourceType).embedding(embedding).build();
        memoryMapper.insert(memory);
        int currentCount = memoryMapper.countByUserId(userId);
        if (currentCount > MAX_MEMORIES) {
            int excess = currentCount - MAX_MEMORIES;
            List<Long> leastRelevant = memoryMapper.findLeastRelevantIds(userId, excess);
            if (leastRelevant != null && !leastRelevant.isEmpty()) {
                memoryMapper.deleteBatchIds(leastRelevant);
                log.info("Evicted {} least-relevant memories: userId={}", leastRelevant.size(), userId);
            } else {
                memoryMapper.deleteOldestByUserId(userId, excess);
            }
        }
    }

    @Override
    public List<LongTermMemory> getRecentMemories(Long userId, int limit) {
        return memoryMapper.findRecentByUserId(userId, limit);
    }

    @Override
    public int countMemories(Long userId) {
        return memoryMapper.countByUserId(userId);
    }

    @Override
    public boolean isDuplicate(Long userId, String content) {
        if (content == null || content.isBlank()) return true;
        List<LongTermMemory> recent = memoryMapper.findRecentByUserId(userId, 30);
        String trimmed = content.trim();
        for (LongTermMemory m : recent)
            if (m.getContent() != null && (m.getContent().contains(trimmed) || trimmed.contains(m.getContent()))) return true;
        return false;
    }

    @Override
    public List<LongTermMemory> searchSimilarMemories(Long userId, String queryText, int limit) {
        if (queryText == null || queryText.isBlank()) return getRecentMemories(userId, limit);
        if (embeddingModel != null) {
            try {
                float[] emb = embeddingModel.embed(queryText);
                String vecStr = vectorToString(emb);
                List<LongTermMemory> results = memoryMapper.searchByVector(userId, vecStr, limit);
                if (results != null && !results.isEmpty()) return results;
            } catch (Exception e) { log.warn("Semantic search failed, falling back", e); }
        }
        return getRecentMemories(userId, limit);
    }

    private static String vectorToString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }
}
