package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.InterviewRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewRecordMapper extends BaseMapper<InterviewRecord> {

    @Select("SELECT * FROM interview_records WHERE user_id = #{userId} ORDER BY completed_at DESC")
    List<InterviewRecord> findByUserIdOrderByCompletedAtDesc(Long userId);

    @Select("SELECT * FROM interview_records WHERE user_id = #{userId} AND completed_at BETWEEN #{start} AND #{end} ORDER BY completed_at DESC")
    List<InterviewRecord> findByUserIdAndCompletedAtBetween(Long userId, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
