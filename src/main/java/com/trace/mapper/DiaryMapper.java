package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.Diary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DiaryMapper extends BaseMapper<Diary> {

    @Select("SELECT * FROM diaries WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Diary> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Select("SELECT * FROM diaries WHERE user_id = #{userId} AND created_at BETWEEN #{start} AND #{end} ORDER BY created_at DESC")
    List<Diary> findByUserIdAndCreatedAtBetween(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
