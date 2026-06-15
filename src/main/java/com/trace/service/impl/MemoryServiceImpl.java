package com.trace.service.impl;

import com.trace.entity.ChatHistory;
import com.trace.entity.LongTermMemory;
import com.trace.mapper.ChatHistoryMapper;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 三层记忆服务实现：
 * <p>
 * Redis 短期上下文（Prompt 注入，10-20 条）<br>
 * PostgreSQL 会话历史（前端展示，分页查询，≤500 条）<br>
 * PostgreSQL 长期记忆（特征摘要，≤30 条）
 * </p>
 */
@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatHistoryMapper chatHistoryMapper;
    private final LongTermMemoryMapper memoryMapper;

    private static final int MAX_CHAT_HISTORY = 500;
    private static final int MAX_SHORT_TERM = 20;
    private static final int MAX_MEMORIES = 30;
    private static final String REDIS_KEY_PREFIX = "chat:short:";

    public MemoryServiceImpl(RedisTemplate<String, Object> redisTemplate,
                              ChatHistoryMapper chatHistoryMapper,
                              LongTermMemoryMapper memoryMapper) {
        this.redisTemplate = redisTemplate;
        this.chatHistoryMapper = chatHistoryMapper;
        this.memoryMapper = memoryMapper;
    }

    // ==================== 会话历史（PG，前端展示用） ====================

    @Override
    @Transactional
    public void saveChatHistory(Long userId, String role, String content) {
        // 1. Redis 短期上下文（Prompt 注入用）
        String key = REDIS_KEY_PREFIX + userId;
        redisTemplate.opsForList().rightPush(key,
                Map.of("role", role, "content", content));
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_SHORT_TERM) {
            redisTemplate.opsForList().trim(key, -MAX_SHORT_TERM, -1);
        }

        // 2. PG 会话历史（前端展示用）
        int count = chatHistoryMapper.countByUserId(userId);
        if (count >= MAX_CHAT_HISTORY) {
            int excess = count - MAX_CHAT_HISTORY + 1;
            chatHistoryMapper.deleteOldestByUserId(userId, excess);
        }
        ChatHistory record = ChatHistory.builder()
                .userId(userId)
                .role(role)
                .content(content)
                .build();
        chatHistoryMapper.insert(record);
    }

    @Override
    public List<ChatHistory> getRecentChats(Long userId, int limit) {
        return chatHistoryMapper.findRecentByUserId(userId, limit);
    }

    @Override
    public List<ChatHistory> getChatsBefore(Long userId, Long beforeId, int limit) {
        if (beforeId == null) {
            return getRecentChats(userId, limit);
        }
        return chatHistoryMapper.findBefore(userId, beforeId, limit);
    }

    // ==================== 短期上下文（Redis，Prompt 注入用） ====================

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> getChatContext(Long userId) {
        String key = REDIS_KEY_PREFIX + userId;
        try {
            List<Object> entries = redisTemplate.opsForList()
                    .range(key, 0, -1);
            if (entries == null || entries.isEmpty()) {
                return List.of();
            }
            return entries.stream()
                    .map(e -> (Map<String, String>) e)
                    .toList();
        } catch (Exception e) {
            log.debug("Redis deserialize failed, clearing key: {}", key, e);
            redisTemplate.delete(key);
            return List.of();
        }
    }

    // ==================== 长期记忆 ====================

    @Override
    @Transactional
    public void saveMemory(Long userId, String content, String sourceType) {
        if (isDuplicate(userId, content)) {
            log.debug("Duplicate memory skipped: userId={}", userId);
            return;
        }
        int currentCount = memoryMapper.countByUserId(userId);
        if (currentCount >= MAX_MEMORIES) {
            int excess = currentCount - MAX_MEMORIES + 1;
            memoryMapper.deleteOldestByUserId(userId, excess);
        }
        LongTermMemory memory = LongTermMemory.builder()
                .userId(userId)
                .content(content)
                .sourceType(sourceType)
                .build();
        memoryMapper.insert(memory);
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
        if (content == null || content.isBlank()) {
            return true;
        }
        List<LongTermMemory> recent = memoryMapper.findRecentByUserId(userId, 30);
        String trimmed = content.trim();
        for (LongTermMemory m : recent) {
            if (m.getContent() != null
                    && (m.getContent().contains(trimmed)
                    || trimmed.contains(m.getContent()))) {
                return true;
            }
        }
        return false;
    }
}
