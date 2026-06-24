package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.service.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "每日打卡", description = "学习计划每日签到打卡")
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
public class CheckInController {
    private final CheckInService checkInService;

    @Operation(summary = "今日打卡")
    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkIn(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Long> body) {
        Map<String, Object> result = checkInService.checkIn(userId, body.get("planId"));
        String msg = Boolean.TRUE.equals(result.get("checked")) && result.size() == 2
                ? "今日已打卡" : "打卡成功";
        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    @Operation(summary = "本周打卡")
    @GetMapping("/week")
    public ResponseEntity<ApiResponse<Map<String, Object>>> weekStatus(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long planId) {
        return ResponseEntity.ok(ApiResponse.success(checkInService.weekStatus(userId, planId)));
    }

    @Operation(summary = "打卡进度")
    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> progress(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long planId) {
        return ResponseEntity.ok(ApiResponse.success(checkInService.progress(userId, planId)));
    }
}
