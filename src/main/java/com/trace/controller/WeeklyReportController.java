package com.trace.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.entity.WeeklyReport;
import com.trace.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/weekly-report") @RequiredArgsConstructor
public class WeeklyReportController {
    private final WeeklyReportService reportService;
    @GetMapping public ResponseEntity<ApiResponse<IPage<WeeklyReport>>> list(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) { return ResponseEntity.ok(ApiResponse.success(reportService.list(userId,page,size))); }
    @GetMapping("/{id}") public ResponseEntity<ApiResponse<WeeklyReport>> get(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(reportService.getById(id))); }
    @PostMapping("/generate") public ResponseEntity<ApiResponse<WeeklyReport>> generate(@AuthenticationPrincipal Long userId) { return ResponseEntity.ok(ApiResponse.success("已生成",reportService.generateWeeklyReport(userId))); }
    @GetMapping("/{id}/download") public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(reportService.getById(id).getReportUrl())); }
}
