package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.dto.PlanGenerateRequest;
import com.trace.entity.StudyPlan;
import com.trace.service.PlanService;
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
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    /**
     * 生成学习计划（异步：立即返回占位，RabbitMQ 后台生成）
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<StudyPlan>> generate(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PlanGenerateRequest request) {
        StudyPlan plan = planService.startGeneration(userId, request.getGoal());
        return ResponseEntity.ok(ApiResponse.success("计划生成任务已提交，请稍候...", plan));
    }

    /**
     * 检查计划生成状态
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkStatus(@PathVariable Long id) {
        boolean completed = planService.isCompleted(id);
        StudyPlan plan = planService.getById(id);
        Map<String, Object> status = Map.of(
                "planId", id,
                "goal", plan.getGoal(),
                "completed", completed,
                "planUrl", plan.getPlanUrl() != null ? plan.getPlanUrl() : "",
                "planContent", plan.getPlanContent() != null ? plan.getPlanContent() : ""
        );
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 历史计划列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<StudyPlan>>> list(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<StudyPlan> plans = planService.list(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(plans));
    }

    /**
     * 下载计划 PDF
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id) {
        StudyPlan plan = planService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("下载链接", plan.getPlanUrl()));
    }
}
