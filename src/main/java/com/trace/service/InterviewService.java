package com.trace.service;

import com.trace.dto.InterviewAnswerRequest;
import com.trace.dto.InterviewStartRequest;
import com.trace.entity.InterviewQuestionDetail;
import com.trace.entity.InterviewRecord;
import com.trace.repository.InterviewQuestionDetailRepository;
import com.trace.repository.InterviewRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewService {

    private final ChatClient.Builder chatClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InterviewRecordRepository recordRepository;
    private final InterviewQuestionDetailRepository detailRepository;
    private final MemoryService memoryService;
    private final PdfService pdfService;

    private static final String SESSION_KEY_PREFIX = "interview:session:";
    private static final int SESSION_TTL_HOURS = 2;

    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 面试教练。你的职责是：
            1. 根据行业和技能出面试题
            2. 客观评估用户回答，给出 1-10 分
            3. 提供建设性的点评和改进建议
            4. 保持专业、客观、鼓励性的语气
            """;

    /**
     * 开始面试 - 创建会话并返回第一题
     */
    public Map<String, Object> startInterview(Long userId, InterviewStartRequest request) {
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_KEY_PREFIX + sessionId;

        // 存储会话状态到 Redis
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("industry", request.getIndustry());
        session.put("skills", request.getSkills());
        session.put("totalQuestions", request.getQuestionCount());
        session.put("currentQuestion", 0);
        session.put("questions", new ArrayList<Map<String, Object>>());
        session.put("scores", new ArrayList<BigDecimal>());
        session.put("status", "IN_PROGRESS");

        redisTemplate.opsForValue().set(sessionKey, session, SESSION_TTL_HOURS, TimeUnit.HOURS);

        // 生成第一题
        Map<String, Object> firstQuestion = generateQuestion(request.getIndustry(), request.getSkills());
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("question", firstQuestion.get("question"));
        result.put("currentQuestion", 1);
        result.put("totalQuestions", request.getQuestionCount());
        result.put("status", "IN_PROGRESS");

        return result;
    }

    /**
     * 提交回答 - 评分并返回下一题或结束
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> submitAnswer(String sessionId, InterviewAnswerRequest request) {
        String sessionKey = SESSION_KEY_PREFIX + sessionId;
        Map<String, Object> session = (Map<String, Object>) redisTemplate.opsForValue().get(sessionKey);

        if (session == null) {
            throw new IllegalArgumentException("面试会话已过期，请重新开始");
        }

        int currentQuestion = (int) session.get("currentQuestion");
        int totalQuestions = (int) session.get("totalQuestions");
        String answer = request.getAnswer();

        // 获取当前题目
        List<Map<String, Object>> questions = (List<Map<String, Object>>) session.get("questions");
        Map<String, Object> currentQ = questions.get(currentQuestion);

        // AI 评分
        Map<String, Object> evaluation = evaluateAnswer(
                (String) session.get("industry"),
                (List<String>) session.get("skills"),
                (String) currentQ.get("question"),
                answer
        );

        // 记录分数
        List<BigDecimal> scores = (List<BigDecimal>) session.get("scores");
        BigDecimal score = (BigDecimal) evaluation.get("score");
        scores.add(score);

        // 更新当前题目记录
        currentQ.put("userAnswer", answer);
        currentQ.put("score", score);
        currentQ.put("aiComment", evaluation.get("comment"));

        Map<String, Object> result = new HashMap<>();
        result.put("score", score);
        result.put("comment", evaluation.get("comment"));

        currentQuestion++;
        session.put("currentQuestion", currentQuestion);

        if (currentQuestion >= totalQuestions) {
            // 面试结束
            session.put("status", "COMPLETED");
            result.put("isLast", true);
            result.put("nextQuestion", null);

            // 计算平均分
            BigDecimal avgScore = scores.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(scores.size()), 2, RoundingMode.HALF_UP);

            // 持久化到数据库
            Long recordId = saveInterviewRecord(session, avgScore);
            result.put("recordId", recordId);
            result.put("avgScore", avgScore);

            redisTemplate.delete(sessionKey);

            // 异步摘要存入长期记忆
            saveInterviewMemory(session, avgScore);
        } else {
            // 生成下一题
            Map<String, Object> nextQuestion = generateQuestion(
                    (String) session.get("industry"),
                    (List<String>) session.get("skills"));
            questions.add(nextQuestion);
            result.put("isLast", false);
            result.put("nextQuestion", nextQuestion.get("question"));
        }

        redisTemplate.opsForValue().set(sessionKey, session, SESSION_TTL_HOURS, TimeUnit.HOURS);

        return result;
    }

    /**
     * 获取面试记录列表
     */
    public org.springframework.data.domain.Page<InterviewRecord> getRecords(Long userId, org.springframework.data.domain.Pageable pageable) {
        return recordRepository.findByUserIdOrderByCompletedAtDesc(userId, pageable);
    }

    /**
     * 获取面试详情
     */
    public InterviewRecord getRecordDetail(Long recordId) {
        InterviewRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("面试记录不存在"));
        List<InterviewQuestionDetail> details = detailRepository.findByRecordIdOrderBySequenceNumAsc(recordId);
        record.setQuestionDetails(details);
        return record;
    }

    /**
     * 生成面试报告 PDF
     */
    public String generateReport(Long recordId) {
        InterviewRecord record = getRecordDetail(recordId);
        StringBuilder content = new StringBuilder();
        content.append("面试行业：").append(record.getIndustry()).append("\n");
        content.append("技能标签：").append(String.join(", ", record.getSkillTags())).append("\n");
        content.append("平均分：").append(record.getAvgScore()).append("\n\n");

        for (var detail : record.getQuestionDetails()) {
            content.append("---\n");
            content.append("第").append(detail.getSequenceNum() + 1).append("题：").append(detail.getQuestion()).append("\n");
            content.append("你的回答：").append(detail.getUserAnswer()).append("\n");
            content.append("AI 点评：").append(detail.getAiComment()).append("\n");
            content.append("得分：").append(detail.getScore()).append("/10\n\n");
        }

        String reportUrl = pdfService.generateAndUpload("面试报告_" + record.getIndustry(), content.toString());
        record.setReportUrl(reportUrl);
        recordRepository.save(record);

        return reportUrl;
    }

    // ============ Private Methods ============

    private Map<String, Object> generateQuestion(String industry, List<String> skills) {
        ChatClient chatClient = chatClientBuilder.build();
        String prompt = String.format(
                "请为 %s 行业的 %s 岗位出一道面试题。要求：1. 题目有深度且实用 2. 适合中高级水平 3. 只输出题目本身",
                industry, String.join("、", skills)
        );

        String question = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        return result;
    }

    private Map<String, Object> evaluateAnswer(String industry, List<String> skills, String question, String answer) {
        ChatClient chatClient = chatClientBuilder.build();
        String prompt = String.format(
                """
                请评估以下面试回答：

                行业：%s
                岗位技能：%s
                题目：%s
                回答：%s

                请给出：
                1. 评分（1-10，只输出数字）
                2. 点评（200字以内，包含优点和改进建议）

                输出格式（JSON）：
                {"score": 8, "comment": "点评内容"}
                """,
                industry, String.join("、", skills), question, answer
        );

        try {
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(prompt)
                    .call()
                    .content();

            // 简单 JSON 解析
            String scoreStr = extractJsonValue(response, "score");
            String comment = extractJsonValue(response, "comment");

            Map<String, Object> result = new HashMap<>();
            result.put("score", new BigDecimal(scoreStr != null ? scoreStr : "5"));
            result.put("comment", comment != null ? comment : "感谢你的回答。");
            return result;
        } catch (Exception e) {
            log.error("Evaluation failed", e);
            Map<String, Object> result = new HashMap<>();
            result.put("score", new BigDecimal("5"));
            result.put("comment", "评分暂时不可用，请继续下一题。");
            return result;
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;

        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;

        int startQuote = json.indexOf("\"", colonIndex);
        if (startQuote == -1) {
            // 数字值
            int start = colonIndex + 1;
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.')) {
                end++;
            }
            return json.substring(start, end).trim();
        }

        int endQuote = json.indexOf("\"", startQuote + 1);
        if (endQuote == -1) return null;

        return json.substring(startQuote + 1, endQuote);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    private Long saveInterviewRecord(Map<String, Object> session, BigDecimal avgScore) {
        InterviewRecord record = InterviewRecord.builder()
                .userId(((Number) session.get("userId")).longValue())
                .industry((String) session.get("industry"))
                .skillTags((List<String>) session.get("skills"))
                .totalQuestions((int) session.get("totalQuestions"))
                .avgScore(avgScore)
                .build();

        record = recordRepository.save(record);

        List<Map<String, Object>> questions = (List<Map<String, Object>>) session.get("questions");
        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> q = questions.get(i);
            InterviewQuestionDetail detail = InterviewQuestionDetail.builder()
                    .recordId(record.getId())
                    .question((String) q.get("question"))
                    .userAnswer((String) q.getOrDefault("userAnswer", ""))
                    .aiComment((String) q.getOrDefault("aiComment", ""))
                    .score((BigDecimal) q.getOrDefault("score", BigDecimal.ZERO))
                    .sequenceNum(i)
                    .build();
            detailRepository.save(detail);
        }

        return record.getId();
    }

    @SuppressWarnings("unchecked")
    private void saveInterviewMemory(Map<String, Object> session, BigDecimal avgScore) {
        try {
            String industry = (String) session.get("industry");
            List<String> skills = (List<String>) session.get("skills");
            String content = String.format(
                    "【面试总结】行业：%s | 技能：%s | 题目数：%d | 平均分：%s",
                    industry,
                    String.join("、", skills),
                    (int) session.get("totalQuestions"),
                    avgScore.toString()
            );
            memoryService.saveLongTermMemory(
                    ((Number) session.get("userId")).longValue(),
                    content,
                    "interview",
                    null
            );
        } catch (Exception e) {
            log.error("Failed to save interview memory", e);
        }
    }
}
