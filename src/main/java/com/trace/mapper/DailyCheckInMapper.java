package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.DailyCheckIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyCheckInMapper extends BaseMapper<DailyCheckIn> {

    @Select("SELECT * FROM daily_check_ins WHERE user_id = #{userId} AND plan_id = #{planId} AND check_date = #{date}")
    DailyCheckIn findByUserPlanDate(@Param("userId") Long userId,
                                     @Param("planId") Long planId,
                                     @Param("date") LocalDate date);

    @Select("SELECT * FROM daily_check_ins WHERE user_id = #{userId} AND check_date BETWEEN #{start} AND #{end} ORDER BY check_date")
    List<DailyCheckIn> findByUserIdAndDateBetween(@Param("userId") Long userId,
                                                   @Param("start") LocalDate start,
                                                   @Param("end") LocalDate end);
}
