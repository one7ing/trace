package com.trace.service;

import com.trace.config.RabbitMQConfig;
import com.trace.entity.StudyPlan;
import com.trace.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    private final StudyPlanRepository planRepository;
    private final RabbitTemplate rabbitTemplate;

    /**
     * 创建占位计划，发送 RabbitMQ 消息，立即返回
     */
    @Transactional
    public StudyPlan startGeneration(Long userId, String goal) {
        log.info("Creating plan placeholder: userId={}, goal={}", userId, goal);

        StudyPlan plan = StudyPlan.builder()
                .userId(userId)
                .goal(goal)
                .planContent("正在生成中...")
                .planUrl(null)
                .build();

        plan = planRepository.save(plan);

        // 发送 RabbitMQ 异步消息
        Map<String, Object> message = Map.of(
                "planId", plan.getId(),
                "userId", userId,
                "goal", goal
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PLAN_EXCHANGE,
                RabbitMQConfig.PLAN_ROUTING_KEY,
                message
        );

        log.info("Plan message sent to RabbitMQ: planId={}", plan.getId());
        return plan;
    }

    public Page<StudyPlan> list(Long userId, Pageable pageable) {
        return planRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public StudyPlan getById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("计划不存在"));
    }

    /**
     * 检查计划生成是否完成
     */
    public boolean isCompleted(Long id) {
        StudyPlan plan = getById(id);
        return plan.getPlanUrl() != null && !"正在生成中...".equals(plan.getPlanContent());
    }
}
