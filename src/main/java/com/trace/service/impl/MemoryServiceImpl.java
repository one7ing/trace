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

    private static final double SIMILARITY_THRESHOLD = 0.70;

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
            log.warn("短期上下文保存至Redis失败: userId={}", userId, e);
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
                } catch (Exception e) { log.debug("跳过无法解析的条目"); }
            }
            return result;
        } catch (Exception e) {
            log.warn("获取短期上下文失败，已清除Redis缓存: userId={}", userId, e);
            stringRedisTemplate.delete(key);
            return List.of();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getChatContext(Long userId, int limit) {
        String key = REDIS_KEY_PREFIX + userId;
        try {
            List<String> entries = stringRedisTemplate.opsForList().range(key, -limit, -1);
            if (entries == null || entries.isEmpty()) return List.of();
            List<Map<String, String>> result = new ArrayList<>();
            for (String json : entries) {
                if (json == null || json.isBlank()) continue;
                try {
                    Map<String, String> map = objectMapper.readValue(json,
                            objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, String.class));
                    result.add(map);
                } catch (Exception e) { log.debug("跳过无法解析的条目"); }
            }
            return result;
        } catch (Exception e) {
            log.warn("获取最近{}条短期上下文失败，已清除Redis缓存: userId={}", limit, userId, e);
            stringRedisTemplate.delete(key);
            return List.of();
        }
    }


    @Override
    @Transactional
    public void saveMemory(Long userId, String content, String sourceType, String embedding) {
        // 空内容直接跳过
        if (content == null || content.isBlank()) return;
            // --- 向量去重：用向量相似度判断是否和已有记忆重复 ---
            // 调用 Mapper，在 long_term_memories 表里搜索最相似的已有记忆
        var similar = memoryMapper.findSimilarMemory(userId, embedding, SIMILARITY_THRESHOLD);

        if (similar != null && !similar.isEmpty()) {
            // 如果找到相似度 > 阈值的记录，说明是同一件事的更新
            Map<String, Object> match = similar.getFirst();
            Long existingId = ((Number) match.get("id")).longValue();
            double sim = ((Number) match.get("similarity")).doubleValue();

            log.info("长期记忆合并: userId={}, 相似度={}, 旧记录ID={}",
                    userId, String.format("%.3f", sim), existingId);

            // 不新增记录，而是更新旧记录的 content 和 embedding
            memoryMapper.updateContentAndEmbedding(existingId, content, embedding);
            return;  // 已经更新，不需要继续往下走
        }

        // ========== 第二步：插入新记忆 ==========
        memoryMapper.insertembding(userId, content, sourceType, embedding);

        // ========== 第三步：容量控制（超出上限直接淘汰最早记录） ==========
        int currentCount = memoryMapper.countByUserId(userId);
        if (currentCount > MAX_MEMORIES) {
            int excess = currentCount - MAX_MEMORIES;
            memoryMapper.deleteOldestByUserId(userId, excess);
            log.info("已淘汰{}条最早长期记忆: userId={}", excess, userId);
        }
    }

    @Override
    public List<LongTermMemory> getRecentMemories(Long userId, int limit) {
        return memoryMapper.findRecentByUserId(userId, limit);
    }


    /**
     * 用于长期记忆注入
     * @param userId
     * @param queryText
     * @param limit
     * @return
     */
    @Override
    public List<LongTermMemory> searchSimilarMemories(Long userId, String queryText, int limit) {
        if (queryText == null || queryText.isBlank())
            return getRecentMemories(userId, limit);
        if (embeddingModel != null) {
            try {
                float[] emb = embeddingModel.embed(queryText);
                String vecStr = vectorToString(emb);
                List<LongTermMemory> results = memoryMapper.searchByVector(userId, vecStr, limit);
                if (results != null && !results.isEmpty())
                    return results;
            } catch (Exception e) { log.warn("语义搜索失败，回退到最近记忆: userId={}", userId, e); }
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
