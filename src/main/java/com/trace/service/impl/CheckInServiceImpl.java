package com.trace.service.impl;

import com.trace.entity.DailyCheckIn;
import com.trace.mapper.DailyCheckInMapper;
import com.trace.service.CheckInService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final DailyCheckInMapper checkInMapper;

    @Override
    @Transactional
    public Map<String, Object> checkIn(Long userId, Long planId) {
        LocalDate today = LocalDate.now();
        // 去重检查
        DailyCheckIn existing = checkInMapper.findByUserPlanDate(userId, planId, today);
        if (existing != null) {
            return Map.of("checked", true, "date", today.toString());
        }
        // 写入打卡记录
        DailyCheckIn ci = DailyCheckIn.builder()
                .userId(userId).planId(planId)
                .checkDate(today).createdAt(LocalDateTime.now())
                .build();
        checkInMapper.insert(ci);
        return Map.of("checked", true, "date", today.toString());
    }

    @Override
    public Map<String, Object> weekStatus(Long userId, Long planId) {
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyCheckIn> weekChecks = checkInMapper
                .findByUserIdAndDateBetween(userId, weekStart, weekEnd);
        if (planId != null) {
            weekChecks = weekChecks.stream()
                    .filter(c -> planId.equals(c.getPlanId())).toList();
        }

        Set<String> checkedDates = new LinkedHashSet<>();
        for (DailyCheckIn c : weekChecks) checkedDates.add(c.getCheckDate().toString());

        String[] dayNames = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = weekStart.plusDays(i);
            Map<String, Object> dayInfo = new LinkedHashMap<>();
            dayInfo.put("day", dayNames[i]);
            dayInfo.put("date", d.toString());
            dayInfo.put("checked", checkedDates.contains(d.toString()));
            days.add(dayInfo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("weekStart", weekStart.toString());
        result.put("weekEnd", weekEnd.toString());
        result.put("days", days);
        result.put("totalChecked", weekChecks.size());
        return result;
    }

    @Override
    public Map<String, Object> progress(Long userId, Long planId) {
        List<DailyCheckIn> all = checkInMapper.findByUserIdAndDateBetween(
                userId, LocalDate.of(2000, 1, 1), LocalDate.now());
        long totalChecked = all.stream()
                .filter(c -> planId.equals(c.getPlanId())).count();

        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DailyCheckIn> weekChecks = checkInMapper
                .findByUserIdAndDateBetween(userId, weekStart, LocalDate.now());
        long weekChecked = weekChecks.stream()
                .filter(c -> planId.equals(c.getPlanId())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalChecked", totalChecked);
        result.put("weekChecked", weekChecked);
        return result;
    }
}
