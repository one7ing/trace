package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.WeeklyReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WeeklyReportMapper extends BaseMapper<WeeklyReport> {

    @Select("SELECT * FROM weekly_reports WHERE user_id = #{userId} ORDER BY week_start DESC")
    List<WeeklyReport> findByUserIdOrderByWeekStartDesc(Long userId);

    @Select("SELECT * FROM weekly_reports WHERE user_id = #{userId} AND week_start = #{weekStart}")
    WeeklyReport findByUserIdAndWeekStart(Long userId, java.time.LocalDate weekStart);
}
