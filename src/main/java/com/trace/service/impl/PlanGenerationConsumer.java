package com.trace.service.impl;

import com.trace.agent.Agent;
import com.trace.config.RabbitMQConfig;
import com.trace.entity.StudyPlan;
import com.trace.mapper.StudyPlanMapper;
import com.trace.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.constant.constant.RabbitMQ.PLAN_QUEUE;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanGenerationConsumer {
    private final StudyPlanMapper planMapper;
    private final PdfService pdfService;
    private final List<Agent> agents;
    private final PlanSseRegistry sseRegistry;

    @RabbitListener(queues = PLAN_QUEUE)
    @Transactional
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
            plan.setPlanContent(content);
            plan.setPlanUrl(url);
            planMapper.updateById(plan);
            sseRegistry.pushCompleted(planId, Map.of(
                    "planId", planId, "goal", goal,
                    "planContent", content, "planUrl", url,
                    "totalDuration", plan.getTotalDuration() != null ? plan.getTotalDuration() : 0));
            log.info("RabbitMQ: plan completed planId={}", planId);
        } catch (Exception e) {
            log.error("RabbitMQ: plan failed planId={}", planId, e);
            String errMsg = "生成失败：" + e.getMessage();
            plan.setPlanContent(errMsg);
            plan.setPlanUrl("");
            planMapper.updateById(plan);
            sseRegistry.pushCompleted(planId, Map.of(
                    "planId", planId, "goal", goal,
                    "planContent", errMsg, "planUrl", "",
                    "totalDuration", plan.getTotalDuration() != null ? plan.getTotalDuration() : 0));
        }
    }
}
