package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    @Select("SELECT * FROM long_term_memories WHERE user_id = #{userId} AND source_type IN ('diary','interview','knowledge','plan') ORDER BY created_at DESC")
    List<LongTermMemory> findRecentByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM long_term_memories WHERE user_id = #{userId} AND source_type = #{sourceType} ORDER BY created_at DESC")
    List<LongTermMemory> findByUserIdAndSourceType(Long userId, String sourceType);

    // 向量检索用原生 JDBC（PgVector 的 <=> 运算符 MyBatis-Plus 不直接支持）
}
