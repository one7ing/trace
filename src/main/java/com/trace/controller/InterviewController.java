package com.trace.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.*;
import com.trace.entity.InterviewRecord;
import com.trace.service.InterviewService;
import com.trace.service.impl.InterviewSseRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import java.util.Map;

@RestController @RequestMapping("/api/interview") @RequiredArgsConstructor
public class InterviewController {
    private final InterviewService interviewService;
    private final InterviewSseRegistry sseRegistry;

        // 开始面试（同步）
        @PostMapping("/start")
        public ResponseEntity<ApiResponse<Map<String, Object>>> start(
                @AuthenticationPrincipal Long userId,
                @Valid @RequestBody InterviewStartRequest req) {
            return ResponseEntity.ok(
                    ApiResponse.success(interviewService.startInterview(userId, req)));
        }

        // 提交答案（同步）
        @PostMapping("/answer")
        public ResponseEntity<ApiResponse<Map<String, Object>>> answer(
                @Valid @RequestBody InterviewAnswerRequest req) {
            return ResponseEntity.ok(
                    ApiResponse.success(interviewService.submitAnswer(req.getSessionId(), req)));
        }

        // 分页查询面试记录
        @GetMapping("/records")
        public ResponseEntity<ApiResponse<IPage<InterviewRecord>>> records(
                @AuthenticationPrincipal Long userId,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "10") int size) {
            return ResponseEntity.ok(
                    ApiResponse.success(interviewService.getRecords(userId, page, size)));
        }

        // 查看面试详情
        @GetMapping("/records/{id}")
        public ResponseEntity<ApiResponse<InterviewRecord>> detailShort(
                @PathVariable Long id) {
            return ResponseEntity.ok(
                    ApiResponse.success(interviewService.getRecordDetail(id)));
        }

        // 查看面试详情
        @GetMapping("/records/{id}/details")
        public ResponseEntity<ApiResponse<InterviewRecord>> detail(
                @PathVariable Long id) {
            return ResponseEntity.ok(
                    ApiResponse.success(interviewService.getRecordDetail(id)));
        }

        /** SSE 端点：等待面试评价生成完成 */
        @GetMapping(value = "/records/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public SseEmitter stream(@PathVariable Long id) {
            SseEmitter emitter = sseRegistry.register(id);
            // 如果已经完成，直接推送
            InterviewRecord r = interviewService.getRecordDetail(id);
            if (r.getAiAnalysis() != null && !r.getAiAnalysis().isBlank()) {
                Map<String, Object> data = Map.of(
                        "recordId", id,
                        "aiAnalysis", r.getAiAnalysis());
                try {
                    emitter.send(SseEmitter.event().name("completed").data(data));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
            return emitter;
        }

        // 下载面试报告
        @GetMapping("/report/{id}/download")
        public ResponseEntity<ApiResponse<String>> download(
                @PathVariable Long id) {
            return ResponseEntity.ok(
                    ApiResponse.success(interviewService.generateReport(id)));
        }

        // 中断面试
        @PostMapping("/{sessionId}/abort")
        public ResponseEntity<ApiResponse<String>> abort(
                @PathVariable("sessionId") String sessionId) {
            interviewService.abortInterview(sessionId);
            return ResponseEntity.ok(ApiResponse.success("面试已中断"));
        }

        // 删除面试记录
        @DeleteMapping("/records/{id}")
        public ResponseEntity<ApiResponse<String>> deleteRecord(
                @PathVariable Long id) {
            interviewService.deleteRecord(id);
            return ResponseEntity.ok(ApiResponse.success("记录已删除"));
        }

        // 流式开始面试
        @PostMapping(value = "/start-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<String> startStream(
                @AuthenticationPrincipal Long userId,
                @Valid @RequestBody InterviewStartRequest req) {
            return interviewService.startInterviewStream(userId, req);
        }

        // 流式提交答案
        @PostMapping(value = "/answer-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        public Flux<String> answerStream(
                @Valid @RequestBody InterviewAnswerRequest req) {
            return interviewService.submitAnswerStream(req.getSessionId(), req);
        }
}

