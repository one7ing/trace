package com.trace.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.dto.PlanGenerateRequest;
import com.trace.entity.StudyPlan;
import com.trace.service.PlanService;
import com.trace.service.PlanSseRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController @RequestMapping("/api/plan") @RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;
    private final PlanSseRegistry sseRegistry;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<StudyPlan>> generate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlanGenerateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("已提交",
                planService.startGeneration(userId, req.getGoal())));
    }

    /** SSE 端点：等待计划生成完成 */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        SseEmitter emitter = sseRegistry.register(id);
        // 如果已经完成，直接推送
        if (planService.isCompleted(id)) {
            StudyPlan p = planService.getById(id);
            Map<String, Object> data = Map.of(
                    "planId", id, "goal", p.getGoal(),
                    "planContent", p.getPlanContent() != null ? p.getPlanContent() : "",
                    "planUrl", p.getPlanUrl() != null ? p.getPlanUrl() : "");
            try {
                emitter.send(SseEmitter.event().name("completed").data(data));
                emitter.complete();
            } catch (Exception ignored) {}
        }
        return emitter;
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> status(@PathVariable Long id) {
        boolean ok = planService.isCompleted(id);
        StudyPlan p = planService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "planId", id, "goal", p.getGoal(), "completed", ok,
                "planUrl", p.getPlanUrl() != null ? p.getPlanUrl() : "",
                "planContent", p.getPlanContent() != null ? p.getPlanContent() : "")));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<IPage<StudyPlan>>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(planService.list(userId, page, size)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(planService.getById(id).getPlanUrl()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return ResponseEntity.ok(ApiResponse.success("已删除"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<StudyPlan>> updatePlan(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String content = body.get("planContent");
        return ResponseEntity.ok(ApiResponse.success("已保存", planService.updatePlan(id, content)));
    }

    @PostMapping("/{id}/regenerate-pdf")
    public ResponseEntity<ApiResponse<String>> regeneratePdf(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("PDF已重新生成", planService.regeneratePdf(id)));
    }
}
