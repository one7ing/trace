package com.trace.service.impl;

import com.trace.config.RabbitMQConfig;
import com.trace.entity.InterviewRecord;
import com.trace.mapper.InterviewRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 面试综合评价消费者 —— RabbitMQ 异步生成面试评价。
 * Q&A 数据随消息传入，不持久化到数据库。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewEvaluationConsumer {

    private final ChatClient.Builder chatClientBuilder;
    private final InterviewRecordMapper recordMapper;
    private final InterviewSseRegistry sseRegistry;

    @RabbitListener(queues = RabbitMQConfig.INTERVIEW_EVAL_QUEUE)
    @Transactional
    @SuppressWarnings("unchecked")
    public void handleEvaluation(Map<String, Object> msg) {
        Long recordId = Long.valueOf(msg.get("recordId").toString());
        String industry = (String) msg.getOrDefault("industry", "");
        List<Map<String, Object>> questions =
                (List<Map<String, Object>>) msg.get("questions");
        log.info("RabbitMQ: generating evaluation for recordId={}", recordId);

        InterviewRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            log.error("Interview record not found: {}", recordId);
            return;
        }

        // 构建评价上下文
        StringBuilder ctx = new StringBuilder();
        ctx.append("行业：").append(industry).append("\n\n");
        if (questions != null) {
            for (int i = 0; i < questions.size(); i++) {
                Map<String, Object> q = questions.get(i);
                ctx.append("第").append(i + 1).append("题：")
                        .append(q.getOrDefault("question", "")).append("\n");
                Object answer = q.get("answer");
                if (answer != null && !answer.toString().isBlank()) {
                    ctx.append("回答：").append(answer).append("\n");
                }
            }
        }
        ctx.append("\n请基于整场面试记录，给出全面、建设性的综合点评"
                + "（使用 Markdown 格式，包含以下四部分）：\n"
                + "## 整体表现\n"
                + "对候选人的综合印象和表现概括（2-3句话）\n\n"
                + "## 亮点与优势\n"
                + "具体指出哪些问题回答得好，"
                + "技术基础哪些方面扎实，"
                + "展现出了什么能力（逐条列出）\n\n"
                + "## 需要改进\n"
                + "具体指出哪些方面薄弱，"
                + "知识点缺失或理解偏差，"
                + "表达/逻辑/深度等方面的不足（逐条列出）\n\n"
                + "## 学习建议\n"
                + "针对薄弱环节给出具体学习路径、"
                + "推荐资源和练习方向");

        try {
            String finalEval = chatClientBuilder.build().prompt()
                    .system("你是资深面试官。请基于整场面试记录，"
                            + "给出全面、建设性、有温度的综合点评。"
                            + "既要点出亮点给候选人信心，"
                            + "也要坦诚指出需要改进的地方。使用 Markdown 格式。")
                    .user(ctx.toString())
                    .call().content();

            if (finalEval == null || finalEval.isBlank()) {
                finalEval = "## 评价生成失败\n\nAI 未能生成有效评价，请稍后重试。";
            }
            record.setAiAnalysis(finalEval);
        } catch (Exception e) {
            log.error("AI evaluation failed for recordId={}", recordId, e);
            record.setAiAnalysis("## 评价生成失败\n\nAI 评价生成异常：" + e.getMessage()
                    + "\n\n请稍后重试或联系管理员。");
        }

        recordMapper.updateById(record);

        // 推送 SSE 通知前端
        sseRegistry.pushCompleted(recordId, Map.of(
                "recordId", recordId,
                "aiAnalysis", record.getAiAnalysis()));

        log.info("RabbitMQ: evaluation completed for recordId={}", recordId);
    }
}
