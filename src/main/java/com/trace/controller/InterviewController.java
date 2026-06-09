package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.dto.InterviewAnswerRequest;
import com.trace.dto.InterviewStartRequest;
import com.trace.entity.InterviewRecord;
import com.trace.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    /**
     * 开始面试
     */
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startInterview(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InterviewStartRequest request) {
        Map<String, Object> result = interviewService.startInterview(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 提交回答
     */
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnswer(
            @Valid @RequestBody InterviewAnswerRequest request) {
        Map<String, Object> result = interviewService.submitAnswer(request.getSessionId(), request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 面试记录列表
     */
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Page<InterviewRecord>>> getRecords(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<InterviewRecord> records = interviewService.getRecords(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(records));
    }

    /**
     * 面试详情
     */
    @GetMapping("/records/{id}/details")
    public ResponseEntity<ApiResponse<InterviewRecord>> getRecordDetail(@PathVariable Long id) {
        InterviewRecord record = interviewService.getRecordDetail(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    /**
     * 下载面试报告 PDF
     */
    @GetMapping("/report/{id}/download")
    public ResponseEntity<ApiResponse<String>> downloadReport(@PathVariable Long id) {
        String reportUrl = interviewService.generateReport(id);
        return ResponseEntity.ok(ApiResponse.success("报告已生成", reportUrl));
    }
}
