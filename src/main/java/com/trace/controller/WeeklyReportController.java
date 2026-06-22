package com.trace.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.entity.WeeklyReport;
import com.trace.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "周报生成", description = "周报列表、手动/定时生成、PDF 下载")
@RestController @RequestMapping("/api/weekly-report") @RequiredArgsConstructor
public class WeeklyReportController {
    private final WeeklyReportService reportService;
    @Operation(summary = "周报列表") @GetMapping public ResponseEntity<ApiResponse<IPage<WeeklyReport>>> list(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) { return ResponseEntity.ok(ApiResponse.success(reportService.list(userId,page,size))); }
    @Operation(summary = "周报详情") @GetMapping("/{id}") public ResponseEntity<ApiResponse<WeeklyReport>> get(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(reportService.getById(id))); }
    @Operation(summary = "生成周报") @PostMapping("/generate") public ResponseEntity<ApiResponse<WeeklyReport>> generate(@AuthenticationPrincipal Long userId) { return ResponseEntity.ok(ApiResponse.success("已生成",reportService.generateWeeklyReport(userId))); }
    @Operation(summary = "下载周报 PDF") @GetMapping("/{id}/download") public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(reportService.getById(id).getReportUrl())); }
}
