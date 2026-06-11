package com.trace.service;

import com.trace.agent.Agent;
import com.trace.config.RabbitMQConfig;
import com.trace.entity.StudyPlan;
import com.trace.mapper.StudyPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanGenerationConsumer {
    private final StudyPlanMapper planMapper;
    private final MemoryService memoryService;
    private final PdfService pdfService;
    private final List<Agent> agents;

    @RabbitListener(queues = RabbitMQConfig.PLAN_QUEUE) @Transactional
    public void handlePlanGeneration(Map<String, Object> msg) {
        Long planId = Long.valueOf(msg.get("planId").toString());
        Long userId = Long.valueOf(msg.get("userId").toString());
        String goal = (String) msg.get("goal");
        log.info("RabbitMQ: generating plan planId={}", planId);
        StudyPlan plan = planMapper.selectById(planId);
        if (plan == null) { log.error("Plan not found: {}", planId); return; }
        try {
            Agent pa = agents.stream().filter(a -> "plan".equals(a.name())).findFirst().orElse(null);
            String content = pa != null ? pa.handle("请为以下目标制定详细中文学习计划：\n" + goal, userId) : "服务暂不可用";
            String url = pdfService.generateAndUpload("学习计划_" + goal, content);
            plan.setPlanContent(content); plan.setPlanUrl(url); planMapper.updateById(plan);
            memoryService.saveLongTermMemory(userId, "【学习计划】目标：" + goal, "plan", planId);
            log.info("RabbitMQ: plan completed planId={}", planId);
        } catch (Exception e) { log.error("RabbitMQ: plan failed planId={}", planId, e); plan.setPlanContent("生成失败：" + e.getMessage()); planMapper.updateById(plan); }
    }
}
