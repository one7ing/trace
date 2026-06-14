package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.ChatHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 会话历史 Mapper —— 持久化聊天记录到 PostgreSQL。
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * 查询用户最近的 N 条聊天记录。
     *
     * @param userId 用户 ID
     * @param limit  最大条数
     */
    List<ChatHistory> findRecentByUserId(@Param("userId") Long userId,
                                         @Param("limit") int limit);

    /**
     * 查询用户在指定 ID 之前的 N 条记录（用于上滑加载更多）。
     *
     * @param userId 用户 ID
     * @param beforeId 分页游标（查询比此 ID 更早的记录）
     * @param limit  最大条数
     */
    List<ChatHistory> findBefore(@Param("userId") Long userId,
                                 @Param("beforeId") Long beforeId,
                                 @Param("limit") int limit);

    /**
     * 统计用户聊天记录条数。
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 删除用户最旧的 N 条记录（用于容量控制）。
     */
    void deleteOldestByUserId(@Param("userId") Long userId,
                              @Param("count") int count);
}
