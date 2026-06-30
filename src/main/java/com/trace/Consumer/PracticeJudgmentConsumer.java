package com.trace.Consumer;

import com.trace.entity.PracticeQuestionDetail;
import com.trace.entity.PracticeRecord;
import com.trace.mapper.PracticeQuestionDetailMapper;
import com.trace.mapper.PracticeRecordMapper;
import com.trace.service.impl.PracticeSseRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static com.constant.constant.RabbitMQ.PRACTICE_JUDGE_QUEUE;

/**
 * 刷题判题消费者 —— RabbitMQ 异步 AI 判题。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PracticeJudgmentConsumer {

    private final ChatClient.Builder chatClientBuilder;
    private final PracticeRecordMapper recordMapper;
    private final PracticeQuestionDetailMapper detailMapper;
    private final PracticeSseRegistry sseRegistry;

    @RabbitListener(queues = PRACTICE_JUDGE_QUEUE)
    @Transactional
    @SuppressWarnings("unchecked")
    public void handleJudgment(Map<String, Object> msg) {
        Long recordId = Long.valueOf(msg.get("recordId").toString());
        String topic = (String) msg.getOrDefault("topic", "");
        List<Map<String, Object>> questions =
                (List<Map<String, Object>>) msg.get("questions");
        Long userId = msg.get("userId") != null
                ? Long.valueOf(msg.get("userId").toString()) : null;

        log.info("RabbitMQ消费者: 开始判题 recordId={}", recordId);

        PracticeRecord record = recordMapper.selectById(recordId);
        if (record == null) {
            log.error("刷题记录未找到: recordId={}", recordId);
            return;
        }

        int correctCount = 0;
        BigDecimal totalScore = BigDecimal.ZERO;

        try {
            for (Map<String, Object> q : questions) {
                int seq = (Integer) q.get("sequenceNum");
                String question = (String) q.get("question");
                String refAnswer = (String) q.getOrDefault("referenceAnswer", "");
                String userAnswer = (String) q.getOrDefault("userAnswer", "");

                if (userAnswer == null || userAnswer.isBlank()) {
                    // 未作答
                    saveDetail(recordId, question, refAnswer, "",
                            false, BigDecimal.ZERO, "未作答", seq);
                    continue;
                }

                // AI 判题：参考答案非空时仅参考，为空时独立判断
                StringBuilder ctx = new StringBuilder();
                ctx.append("## 题目\n").append(question).append("\n\n");
                boolean hasRef = refAnswer != null && !refAnswer.isBlank();
                if (hasRef) {
                    ctx.append("## 参考答案（仅供参考）\n").append(refAnswer).append("\n\n");
                }
                ctx.append("## 用户答案\n").append(userAnswer).append("\n\n");
                ctx.append("请分析用户答案是否正确，按以下 JSON 格式回复：\n");
                ctx.append("{\"correct\": true/false, \"score\": 8, "
                        + "\"comment\": \"点评（50字以内）\"}");

                String sysPrompt;
                if (hasRef) {
                    sysPrompt = "你是刷题判官。你的任务是对比用户答案与参考答案的关键知识点：\n"
                            + "1. 参考答案仅为参考，用户言之有理即可算对\n"
                            + "2. 不需要逐字匹配，意思到了即可\n"
                            + "3. 完全跑题或遗漏所有重点算错\n"
                            + "4. 评分标准：说全8-10分，说一半5-7分，基本没说0分\n"
                            + "5. 点评要具体：指出说到了哪些点、遗漏了哪些点\n"
                            + "仅输出 JSON，不要其他内容。";
                } else {
                    sysPrompt = "你是刷题判官。题目没有参考答案，请根据你的专业知识独立判断用户答案：\n"
                            + "1. 答案是否在知识上正确、完整\n"
                            + "2. 评分标准：准确全面8-10分，部分正确5-7分，基本错误1-4分\n"
                            + "3. 点评要具体：指出对在哪里、错在哪里\n"
                            + "仅输出 JSON，不要其他内容。";
                }

                String result = chatClientBuilder.build().prompt()
                        .system(sysPrompt)
                        .user(ctx.toString())
                        .call().content();

                boolean isCorrect = false;
                BigDecimal score = BigDecimal.ZERO;
                String comment = "";

                try {
                    if (result != null) {
                        // 提取 JSON
                        String json = result.trim();
                        int jsonStart = json.indexOf('{');
                        int jsonEnd = json.lastIndexOf('}');
                        if (jsonStart >= 0 && jsonEnd >= 0) {
                            json = json.substring(jsonStart, jsonEnd + 1);
                        }
                        isCorrect = extractBool(json, "correct");
                        score = new BigDecimal(extractString(json, "score"))
                                .setScale(1, RoundingMode.HALF_UP);
                        comment = extractString(json, "comment");
                    }
                } catch (Exception e) {
                    log.warn("判题JSON解析失败: recordId={}, seq={}, 回退判断",
                            recordId, seq, e);
                }

                if (isCorrect) correctCount++;
                totalScore = totalScore.add(score);
                saveDetail(recordId, question, refAnswer, userAnswer,
                        isCorrect, score, comment, seq);
            }

            // 更新记录汇总
            int total = questions.size();
            BigDecimal avg = total > 0
                    ? totalScore.divide(new BigDecimal(total), 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            StringBuilder summary = new StringBuilder();
            summary.append("## 刷题结果\n\n");
            summary.append("- **方向**：").append(topic).append("\n");
            summary.append("- **总题数**：").append(total).append("\n");
            summary.append("- **答对**：").append(correctCount).append("\n");
            summary.append("- **正确率**：")
                    .append(total > 0 ? correctCount * 100 / total : 0)
                    .append("%\n");
            summary.append("- **平均分**：").append(avg).append("/10\n");

            record.setCorrectCount(correctCount);
            record.setScore(avg);
            record.setAiAnalysis(summary.toString());
        } catch (Exception e) {
            log.error("RabbitMQ消费者: 判题失败 recordId={}, 错误位置=PracticeJudgmentConsumer.handleJudgment",
                    recordId, e);
            record.setAiAnalysis("## 判题失败\n\nAI 判题过程出现异常，请稍后重试。");
            record.setCorrectCount(0);
            record.setScore(BigDecimal.ZERO);
        }

        recordMapper.updateById(record);

        // SSE 推送
        sseRegistry.pushCompleted(recordId, Map.of(
                "recordId", recordId,
                "aiAnalysis", record.getAiAnalysis(),
                "correctCount", record.getCorrectCount(),
                "score", record.getScore()));

        log.info("RabbitMQ消费者: 判题完成 recordId={}, 答对{}/{}",
                recordId, record.getCorrectCount(), record.getTotalQuestions());
    }

    @Transactional
    public void saveDetail(Long recordId, String question, String refAnswer,
                           String userAnswer, boolean isCorrect,
                           BigDecimal score, String comment, int seq) {
        PracticeQuestionDetail d = PracticeQuestionDetail.builder()
                .recordId(recordId)
                .question(question)
                .referenceAnswer(refAnswer)
                .userAnswer(userAnswer)
                .isCorrect(isCorrect)
                .score(score)
                .aiComment(comment)
                .sequenceNum(seq)
                .build();
        detailMapper.insert(d);
    }

    private boolean extractBool(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern)
                    .matcher(json);
            return m.find() && "true".equals(m.group(1));
        } catch (Exception e) {
            return false;
        }
    }

    private String extractString(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern)
                    .matcher(json);
            if (m.find()) return m.group(1);
            // 数字类型
            pattern = "\"" + key + "\"\\s*:\\s*([0-9.]+)";
            m = java.util.regex.Pattern.compile(pattern).matcher(json);
            if (m.find()) return m.group(1);
        } catch (Exception e) {
            // fallback
        }
        return "5";
    }
}
