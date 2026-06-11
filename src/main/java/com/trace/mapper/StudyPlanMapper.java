package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.StudyPlan;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudyPlanMapper extends BaseMapper<StudyPlan> {

    @Select("SELECT * FROM study_plans WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<StudyPlan> findByUserIdOrderByCreatedAtDesc(Long userId);
}
