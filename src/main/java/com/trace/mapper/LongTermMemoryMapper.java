package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import com.trace.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 长期记忆 Mapper —— 纯文本存储，按时间倒序检索。
 */
@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    /**
     * 查询用户最近的 N 条长期记忆。
     *
     * @param userId 用户 ID
     * @param limit  最大条数
     * @return 按 created_at 倒序排列的记忆列表
     */
    List<LongTermMemory> findRecentByUserId(@Param("userId") Long userId,
                                            @Param("limit") int limit);

    /**
     * 统计用户长期记忆条数。
     *
     * @param userId 用户 ID
     * @return 总条数
     */
    int countByUserId(@Param("userId") Long userId);

    /**
     * 删除用户最旧的 N 条记忆（用于截断超量数据）。
     *
     * @param userId 用户 ID
     * @param count  要删除的最旧条数
     */
    void deleteOldestByUserId(@Param("userId") Long userId,
                              @Param("count") int count);
}
