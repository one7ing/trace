package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.entity.DailyCheckIn;
import com.trace.mapper.DailyCheckInMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "每日打卡", description = "学习计划每日签到打卡")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {

    private final DailyCheckInMapper checkInMapper;

    @Operation(summary = "今日打卡", description = "对当前活跃计划进行每日签到")
    @PostMapping("")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIn(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Long> body) {
        Long planId = body.get("planId");
        LocalDate today = LocalDate.now();

        DailyCheckIn existing = checkInMapper.findByUserPlanDate(userId, planId, today);
        if (existing != null) {
            return ResponseEntity.ok(ApiResponse.success("今日已打卡",
                    Map.of("checked", true, "date", today.toString())));
        }

        DailyCheckIn ci = DailyCheckIn.builder()
                .userId(userId).planId(planId)
                .checkDate(today)
                .createdAt(LocalDateTime.now())
                .build();
        checkInMapper.insert(ci);

        return ResponseEntity.ok(ApiResponse.success("打卡成功",
                Map.of("checked", true, "date", today.toString())));
    }

    @Operation(summary = "本周打卡", description = "查看本周每日打卡状态（周一至周日）")
    @GetMapping("/week")
    public ResponseEntity<ApiResponse<Map<String, Object>>> weekStatus(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long planId) {
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<DailyCheckIn> weekChecks = checkInMapper
                .findByUserIdAndDateBetween(userId, weekStart, weekEnd);

        // 按计划过滤
        if (planId != null) {
            weekChecks = weekChecks.stream()
                    .filter(c -> planId.equals(c.getPlanId()))
                    .collect(Collectors.toList());
        }

        Set<String> checkedDates = weekChecks.stream()
                .map(c -> c.getCheckDate().toString())
                .collect(Collectors.toSet());

        // 本周每天打卡状态
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

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** 获取指定计划的打卡进度 */
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> progress(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long planId) {
        // 总打卡次数
        List<DailyCheckIn> all = checkInMapper.findByUserIdAndDateBetween(
                userId, LocalDate.of(2000, 1, 1), LocalDate.now());
        long totalChecked = all.stream()
                .filter(c -> c.getPlanId() != null && c.getPlanId().equals(planId))
                .count();

        // 本周打卡
        LocalDate weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<DailyCheckIn> weekChecks = checkInMapper
                .findByUserIdAndDateBetween(userId, weekStart, LocalDate.now());
        long weekChecked = weekChecks.stream()
                .filter(c -> c.getPlanId() != null && c.getPlanId().equals(planId))
                .count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalChecked", totalChecked);
        result.put("weekChecked", weekChecked);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
