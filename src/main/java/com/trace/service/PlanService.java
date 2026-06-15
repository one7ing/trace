package com.trace.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.entity.StudyPlan;

public interface PlanService {
    StudyPlan startGeneration(Long userId, String goal);
    IPage<StudyPlan> list(Long userId, int page, int size);
    StudyPlan getById(Long id);
    boolean isCompleted(Long id);
    void deletePlan(Long id);
    StudyPlan updatePlan(Long id, String planContent);
    String regeneratePdf(Long id);
}
