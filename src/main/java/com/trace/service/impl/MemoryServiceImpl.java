package com.trace.service.impl;

import com.trace.entity.ChatHistory;
import com.trace.entity.LongTermMemory;
import com.trace.mapper.ChatHistoryMapper;
import com.trace.mapper.LongTermMemoryMapper;
import com.trace.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 双层记忆服务实现 —— PostgreSQL 会话历史 + PostgreSQL 长期记忆。
 */
@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private final ChatHistoryMapper chatHistoryMapper;
    private final LongTermMemoryMapper memoryMapper;

    private static final int MAX_CHAT_HISTORY = 500;
    private static final int MAX_MEMORIES = 30;
    private static final int CONTEXT_ROUNDS = 10;

    public MemoryServiceImpl(ChatHistoryMapper chatHistoryMapper,
                              LongTermMemoryMapper memoryMapper) {
        this.chatHistoryMapper = chatHistoryMapper;
        this.memoryMapper = memoryMapper;
    }

    // ==================== 会话历史 ====================

    @Override
    @Transactional
    public void saveChatHistory(Long userId, String role, String content) {
        // 容量控制
        int count = chatHistoryMapper.countByUserId(userId);
        if (count >= MAX_CHAT_HISTORY) {
            int excess = count - MAX_CHAT_HISTORY + 1;
            chatHistoryMapper.deleteOldestByUserId(userId, excess);
            log.debug("Trimmed {} oldest chat records for userId={}", excess, userId);
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

    @Override
    public List<Map<String, String>> getChatContext(Long userId) {
        // 取最近 10 轮（20 条），按时间正序返回
        List<ChatHistory> records = chatHistoryMapper.findRecentByUserId(
                userId, CONTEXT_ROUNDS * 2);
        // 反转成正序
        List<Map<String, String>> result = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            ChatHistory r = records.get(i);
            Map<String, String> map = new LinkedHashMap<>();
            map.put("role", r.getRole());
            map.put("content", r.getContent());
            result.add(map);
        }
        return result;
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
