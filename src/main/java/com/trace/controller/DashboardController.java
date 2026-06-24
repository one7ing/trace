package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "成长仪表盘", description = "综合数据面板、成长评分、趋势图、热力图")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @Operation(summary = "综合仪表盘")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> dashboard(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getDashboard(userId)));
    }

    @Operation(summary = "成长力数据")
    @GetMapping("/growth")
    public ResponseEntity<ApiResponse<Map<String, Object>>> growth(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(dashboardService.getGrowth(userId)));
    }

    @Operation(summary = "添加成长锚点")
    @PostMapping("/anchor")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addAnchor(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.addAnchor(userId, body.get("date"), body.get("label"))));
    }

    @Operation(summary = "删除成长锚点")
    @DeleteMapping("/anchor/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAnchor(@PathVariable Long id) {
        dashboardService.deleteAnchor(id);
        return ResponseEntity.ok(ApiResponse.success("已删除"));
    }
}
