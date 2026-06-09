package com.trace.service;

import com.trace.config.RabbitMQConfig;
import com.trace.entity.StudyPlan;
import com.trace.repository.StudyPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlanGenerationConsumer {

    private final ChatClient.Builder chatClientBuilder;
    private final MemoryService memoryService;
    private final PdfService pdfService;
    private final StudyPlanRepository planRepository;

    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 学习计划教练。你的职责是：
            1. 根据用户输入的目标，制定详细、可执行的学习/成长计划
            2. 按阶段、按天拆解任务，附带时间节点
            3. 提供学习资源建议（书籍、在线课程、实践项目等）
            4. 计划要具体、可量化、有挑战但可实现
            5. 输出使用 Markdown 格式
            """;

    @RabbitListener(queues = RabbitMQConfig.PLAN_QUEUE)
    @Transactional
    public void handlePlanGeneration(Map<String, Object> message) {
        Long planId = Long.valueOf(message.get("planId").toString());
        Long userId = Long.valueOf(message.get("userId").toString());
        String goal = (String) message.get("goal");

        log.info("RabbitMQ consumer: starting plan generation planId={}", planId);
        StudyPlan plan = planRepository.findById(planId).orElse(null);
        if (plan == null) {
            log.error("Plan not found: planId={}", planId);
            return;
        }

        try {
            // 检索用户历史背景
            var memories = memoryService.getRecentMemories(userId,
                    java.util.List.of("diary", "interview", "knowledge", "plan"), 10);

            StringBuilder contextBuilder = new StringBuilder();
            if (memories != null && !memories.isEmpty()) {
                contextBuilder.append("## 用户近期学习/成长背景：\n");
                for (var m : memories) {
                    contextBuilder.append("- ").append(m.getContent()).append("\n");
                }
            }

            // 调用大模型生成计划
            ChatClient chatClient = chatClientBuilder.build();
            String planContent = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(contextBuilder + "\n\n请为以下目标制定详细学习计划：\n" + goal)
                    .call()
                    .content();

            // 生成 PDF
            String planUrl = pdfService.generateAndUpload("学习计划_" + goal, planContent);

            // 更新占位记录
            plan.setPlanContent(planContent);
            plan.setPlanUrl(planUrl);
            planRepository.save(plan);

            // 存入长期记忆
            try {
                String memoryContent = "【学习计划】目标：" + goal + " | 已生成详细计划";
                memoryService.saveLongTermMemory(userId, memoryContent, "plan", planId);
            } catch (Exception e) {
                log.error("Failed to save plan memory", e);
            }

            log.info("RabbitMQ consumer: plan generation completed planId={}", planId);
        } catch (Exception e) {
            log.error("RabbitMQ consumer: plan generation failed planId={}", planId, e);
            plan.setPlanContent("生成失败，请重试：" + e.getMessage());
            planRepository.save(plan);
        }
    }
}
