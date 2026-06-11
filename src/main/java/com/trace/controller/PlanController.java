package com.trace.controller;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.dto.ApiResponse;
import com.trace.dto.PlanGenerateRequest;
import com.trace.entity.StudyPlan;
import com.trace.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/plan") @RequiredArgsConstructor
public class PlanController {
    private final PlanService planService;
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<StudyPlan>> generate(@AuthenticationPrincipal Long userId, @Valid @RequestBody PlanGenerateRequest req) {return ResponseEntity.ok(ApiResponse.success("已提交",planService.startGeneration(userId,req.getGoal()))); }
    @GetMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String,Object>>> status(@PathVariable Long id) { boolean ok=planService.isCompleted(id); StudyPlan p=planService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of("planId",id,"goal",p.getGoal(),"completed",ok,"planUrl",p.getPlanUrl()!=null?p.getPlanUrl():"","planContent",p.getPlanContent()!=null?p.getPlanContent():""))); }
    @GetMapping
    public ResponseEntity<ApiResponse<IPage<StudyPlan>>> list(@AuthenticationPrincipal Long userId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="10") int size) { return ResponseEntity.ok(ApiResponse.success(planService.list(userId,page,size))); }
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<String>> download(@PathVariable Long id)
    { return ResponseEntity.ok(ApiResponse.success(planService.getById(id).getPlanUrl())); }
}
