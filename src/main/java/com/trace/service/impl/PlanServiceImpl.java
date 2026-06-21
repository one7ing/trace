package com.trace.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trace.config.RabbitMQConfig;
import com.trace.entity.StudyPlan;
import com.trace.mapper.StudyPlanMapper;
import com.trace.service.PdfService;
import com.trace.service.PlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.constant.constant.RabbitMQ.PLAN_EXCHANGE;
import static com.constant.constant.RabbitMQ.PLAN_ROUTING_KEY;


@Slf4j
@Service
@RequiredArgsConstructor
public class PlanServiceImpl implements PlanService {

    private final StudyPlanMapper planMapper;
    private final RabbitTemplate rabbitTemplate;
    private final PdfService pdfService;

    @Override
    @Transactional
    public StudyPlan startGeneration(Long userId, String goal, Integer totalDuration) {
        StudyPlan p = StudyPlan.builder()
                .userId(userId).goal(goal)
                .planContent("正在生成中...")
                .totalDuration(totalDuration)
                .source("ai")
                .build();
        planMapper.insert(p);
        rabbitTemplate.convertAndSend(PLAN_EXCHANGE, PLAN_ROUTING_KEY,
                Map.of("planId", p.getId(), "userId", userId, "goal", goal));
        return p;
    }

    @Override
    @Transactional
    public StudyPlan createManual(Long userId, String goal, Integer totalDuration, String planContent) {
        StudyPlan p = StudyPlan.builder()
                .userId(userId).goal(goal)
                .planContent(planContent != null ? planContent : "")
                .planUrl("")
                .totalDuration(totalDuration)
                .source("manual")
                .build();
        planMapper.insert(p);
        // 自动生成 PDF
        if (planContent != null && !planContent.isBlank()) {
            String url = pdfService.generateAndUpload("学习计划_" + goal, planContent);
            p.setPlanUrl(url);
        }
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(p);
        return p;
    }

    @Override
    public IPage<StudyPlan> list(Long userId, int page, int size) {
        List<StudyPlan> all = planMapper.findByUserIdOrderByCreatedAtDesc(userId);
        Page<StudyPlan> mp = new Page<>(page + 1, size);
        int s = (int) mp.offset(), e = Math.min(s + (int) mp.getSize(), all.size());
        mp.setRecords(all.subList(Math.min(s, all.size()), e)); mp.setTotal(all.size());
        return mp;
    }

    @Override
    public StudyPlan getById(Long id) {
        StudyPlan p = planMapper.selectById(id);
        if (p == null) throw new IllegalArgumentException("计划不存在");
        return p;
    }

    @Override
    public boolean isCompleted(Long id) {
        StudyPlan p = getById(id);
        if (p.getPlanUrl() != null && !p.getPlanUrl().isEmpty()
                && !"正在生成中...".equals(p.getPlanContent())) {
            return true;
        }
        if (p.getPlanContent() != null && p.getPlanContent().startsWith("生成失败")) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public void deletePlan(Long id) {
        planMapper.deleteById(id);
    }

    @Override
    @Transactional
    public StudyPlan updatePlan(Long id, String planContent) {
        StudyPlan p = getById(id);
        p.setPlanContent(planContent);
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(p);
        return p;
    }

    @Override
    @Transactional
    public String regeneratePdf(Long id) {
        StudyPlan p = getById(id);
        String url = pdfService.generateAndUpload("学习计划_" + p.getGoal(), p.getPlanContent());
        p.setPlanUrl(url);
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(p);
        return url;
    }
}
