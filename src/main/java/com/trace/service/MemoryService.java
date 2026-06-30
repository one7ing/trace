package com.trace.service;

import com.trace.entity.ChatHistory;
import com.trace.entity.LongTermMemory;
import java.util.List;
import java.util.Map;

public interface MemoryService {

    void saveChatHistory(Long userId, String role, String content);
    List<ChatHistory> getRecentChats(Long userId, int limit);
    List<ChatHistory> getChatsBefore(Long userId, Long beforeId, int limit);
    /** 获取全部短期对话上下文 */
    List<Map<String, String>> getChatContext(Long userId);
    /** 获取最近 limit 条短期对话上下文 */
    List<Map<String, String>> getChatContext(Long userId, int limit);

    void saveMemory(Long userId, String content, String sourceType, String embedding);
    List<LongTermMemory> getRecentMemories(Long userId, int limit);
    List<LongTermMemory> searchSimilarMemories(Long userId, String queryText, int limit);
}
