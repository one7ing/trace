package com.trace.service;

import com.trace.entity.LongTermMemory;

import java.util.List;
import java.util.Map;

public interface MemoryService {

    void saveChatContext(Long userId, String role, String content);

    List<Map<String, String>> getChatContext(Long userId);

    void saveLongTermMemory(Long userId, String content, String sourceType, Long sourceId);

    List<LongTermMemory> retrieveMemories(Long userId, String query, int limit);

    List<LongTermMemory> getRecentMemories(Long userId, List<String> sourceTypes, int limit);
}
