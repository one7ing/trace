package com.trace.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.*;
import com.trace.entity.InterviewRecord;
import com.trace.service.InterviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import java.util.Map;

@RestController @RequestMapping("/api/interview") @RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;
    @PostMapping("/start") public ResponseEntity<ApiResponse<Map<String,Object>>> start(@AuthenticationPrincipal Long userId, @Valid @RequestBody InterviewStartRequest req) { return ResponseEntity.ok(ApiResponse.success(interviewService.startInterview(userId,req))); }
    @PostMapping("/answer") public ResponseEntity<ApiResponse<Map<String,Object>>> answer(@Valid @RequestBody InterviewAnswerRequest req) { return ResponseEntity.ok(ApiResponse.success(interviewService.submitAnswer(req.getSessionId(),req))); }
    @GetMapping("/records") public ResponseEntity<ApiResponse<IPage<InterviewRecord>>> records(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) { return ResponseEntity.ok(ApiResponse.success(interviewService.getRecords(userId,page,size))); }
    @GetMapping("/records/{id}/details") public ResponseEntity<ApiResponse<InterviewRecord>> detail(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(interviewService.getRecordDetail(id))); }
    @GetMapping("/report/{id}/download") public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id) { return ResponseEntity.ok(ApiResponse.success(interviewService.generateReport(id))); }
    @PostMapping(value="/start-stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public Flux<String> startStream(@AuthenticationPrincipal Long userId, @Valid @RequestBody InterviewStartRequest req) { return interviewService.startInterviewStream(userId,req); }
    @PostMapping(value="/answer-stream",produces=MediaType.TEXT_EVENT_STREAM_VALUE) public Flux<String> answerStream(@Valid @RequestBody InterviewAnswerRequest req) { return interviewService.submitAnswerStream(req.getSessionId(),req); }
}
