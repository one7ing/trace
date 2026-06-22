package com.trace.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.*;
import com.trace.entity.PracticeQuestionDetail;
import com.trace.entity.PracticeRecord;
import com.trace.mapper.PracticeQuestionDetailMapper;
import com.trace.service.PracticeService;
import com.trace.service.impl.PracticeSseRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Tag(name = "刷题练习", description = "题库抽题、逐题/全部作答、AI 判题、刷题记录")
@RestController @RequestMapping("/api/practice") @RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;
    private final PracticeSseRegistry sseRegistry;
    private final PracticeQuestionDetailMapper detailMapper;

    @Operation(summary = "开始刷题", description = "按方向从题库随机抽取指定数量的题目，返回 sessionId 和题目列表")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Map<String, Object>>> start(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PracticeStartRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(practiceService.startPractice(userId, req)));
    }

    @Operation(summary = "提交答案", description = "逐题模式提交单题答案，全部模式提交所有答案，全部答完后触发 AI 判题")
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> answer(
            @Valid @RequestBody PracticeAnswerRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(practiceService.submitPractice(req.getSessionId(), req)));
    }

    @Operation(summary = "刷题记录列表", description = "分页查询当前用户的刷题记录")
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<IPage<PracticeRecord>>> records(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                ApiResponse.success(practiceService.getRecords(userId, page, size)));
    }

    @Operation(summary = "刷题详情", description = "查看某次刷题的汇总信息（题目数/答对数/平均分）")
    @GetMapping("/records/{id}")
    public ResponseEntity<ApiResponse<PracticeRecord>> detail(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(practiceService.getRecordDetail(id)));
    }

    @Operation(summary = "逐题判题详情", description = "查看某次刷题每道题的题目、参考答案、用户答案、对错、得分、AI点评")
    @GetMapping("/records/{id}/questions")
    public ResponseEntity<ApiResponse<List<PracticeQuestionDetail>>> questions(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(detailMapper.findByRecordId(id)));
    }

    @Operation(summary = "判题进度 SSE", description = "SSE 推送判题完成通知")
    @GetMapping(value = "/records/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        SseEmitter emitter = sseRegistry.register(id);
        PracticeRecord r = practiceService.getRecordDetail(id);
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

    @Operation(summary = "中断刷题", description = "中断当前正在进行的刷题会话")
    @PostMapping("/{sessionId}/abort")
    public ResponseEntity<ApiResponse<String>> abort(
            @PathVariable("sessionId") String sessionId) {
        practiceService.abortPractice(sessionId);
        return ResponseEntity.ok(ApiResponse.success("刷题已中断"));
    }

    @Operation(summary = "删除刷题记录", description = "删除指定的刷题记录")
    @DeleteMapping("/records/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRecord(
            @PathVariable Long id) {
        practiceService.deleteRecord(id);
        return ResponseEntity.ok(ApiResponse.success("记录已删除"));
    }

    @Operation(summary = "开始刷题（SSE流式）", description = "流式版本：按方向从题库随机抽取题目")
    @PostMapping(value = "/start-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> startStream(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PracticeStartRequest req) {
        return practiceService.startPracticeStream(userId, req);
    }

    @Operation(summary = "提交答案（SSE流式）", description = "流式版本：提交答案并获取判题结果")
    @PostMapping(value = "/answer-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> answerStream(
            @Valid @RequestBody PracticeAnswerRequest req) {
        return practiceService.submitPracticeStream(req.getSessionId(), req);
    }
}
