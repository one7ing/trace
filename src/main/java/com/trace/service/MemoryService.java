package com.trace.service;

import com.trace.entity.ChatHistory;
import com.trace.entity.LongTermMemory;
import java.util.List;
import java.util.Map;

public interface MemoryService {

    void saveChatHistory(Long userId, String role, String content);
    List<ChatHistory> getRecentChats(Long userId, int limit);
    List<ChatHistory> getChatsBefore(Long userId, Long beforeId, int limit);
    List<Map<String, String>> getChatContext(Long userId);

    void saveMemory(Long userId, String content, String sourceType);
    void saveMemory(Long userId, String content, String sourceType, String embedding);
    List<LongTermMemory> getRecentMemories(Long userId, int limit);
    int countMemories(Long userId);
    boolean isDuplicate(Long userId, String content);
    List<LongTermMemory> searchSimilarMemories(Long userId, String queryText, int limit);
}
