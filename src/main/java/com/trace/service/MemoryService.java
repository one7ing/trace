package com.trace.service;

import com.trace.entity.ChatHistory;
import com.trace.entity.LongTermMemory;

import java.util.List;
import java.util.Map;

/**
 * 双层记忆服务 —— PostgreSQL 会话历史 + PostgreSQL 长期记忆。
 * <p>
 * 会话历史存储在 chat_history 表，支持分页查询（上滑加载更多）。
 * 长期记忆存储在 long_term_memories 表，按时间倒序取最近 N 条。
 * </p>
 */
public interface MemoryService {

    // ==================== 会话历史（PostgreSQL 持久化） ====================

    /**
     * 保存一条聊天记录到 PostgreSQL。
     * 自动触发容量控制：超过 500 条时删除最旧记录。
     */
    void saveChatHistory(Long userId, String role, String content);

    /**
     * 获取用户最近 N 条聊天记录（按时间倒序）。
     *
     * @param userId 用户 ID
     * @param limit  最大条数
     * @return 最新的在前面
     */
    List<ChatHistory> getRecentChats(Long userId, int limit);

    /**
     * 获取用户在指定 ID 之前的 N 条记录（上滑加载更多）。
     *
     * @param userId   用户 ID
     * @param beforeId 分页游标（比此 ID 更早的记录），null 表示取最新
     * @param limit    最大条数
     * @return 较旧的记录，最新的在前面
     */
    List<ChatHistory> getChatsBefore(Long userId, Long beforeId, int limit);

    /**
     * 获取用户最近对话上下文（用于 Prompt 注入，保持兼容）。
     * 返回最近 10 轮的对话，按时间正序排列。
     */
    List<Map<String, String>> getChatContext(Long userId);

    // ==================== 长期记忆（纯文本） ====================

    /**
     * 保存一条长期记忆（纯文本）。
     * 自动触发容量控制：超过 30 条时删除最旧记录。
     */
    void saveMemory(Long userId, String content, String sourceType);

    /**
     * 获取用户最近的 N 条长期记忆。
     */
    List<LongTermMemory> getRecentMemories(Long userId, int limit);

    /**
     * 获取用户长期记忆总条数。
     */
    int countMemories(Long userId);

    /**
     * 判断给定内容是否与已有长期记忆高度重复。
     */
    boolean isDuplicate(Long userId, String content);
}
