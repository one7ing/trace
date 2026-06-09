package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.entity.WeeklyReport;
import com.trace.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weekly-report")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService reportService;

    /**
     * 历史周报列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<WeeklyReport>>> list(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<WeeklyReport> reports = reportService.list(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    /**
     * 周报详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeeklyReport>> getById(@PathVariable Long id) {
        WeeklyReport report = reportService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(report));
    }

    /**
     * 手动触发生成周报
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<WeeklyReport>> generate(@AuthenticationPrincipal Long userId) {
        WeeklyReport report = reportService.generateWeeklyReport(userId);
        return ResponseEntity.ok(ApiResponse.success("周报已生成", report));
    }

    /**
     * 下载周报 PDF
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id) {
        WeeklyReport report = reportService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("下载链接", report.getReportUrl()));
    }
}
