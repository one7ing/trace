package com.trace.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.dto.PlanGenerateRequest;
import com.trace.entity.StudyPlan;
import com.trace.service.PlanService;
import com.trace.service.impl.PlanSseRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@Tag(name = "学习计划", description = "AI 生成学习计划、手动创建、打卡进度、PDF 报告")
@RestController @RequestMapping("/api/plan") @RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;
    private final PlanSseRegistry sseRegistry;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<StudyPlan>> generate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlanGenerateRequest req) {
        return ResponseEntity.ok(ApiResponse.success("已提交",
                planService.startGeneration(userId, req.getGoal(), req.getTotalDuration())));
    }

    /** 手动创建计划（不走 AI），自动生成 PDF */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<StudyPlan>> create(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, Object> body) {
        String goal = (String) body.get("goal");
        Integer totalDuration = body.get("totalDuration") != null
                ? ((Number) body.get("totalDuration")).intValue() : null;
        String planContent = (String) body.getOrDefault("planContent", "");
        return ResponseEntity.ok(ApiResponse.success("已创建",
                planService.createManual(userId, goal, totalDuration, planContent)));
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
                    "planUrl", p.getPlanUrl() != null ? p.getPlanUrl() : "",
                    "totalDuration", p.getTotalDuration() != null ? p.getTotalDuration() : 0);
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
