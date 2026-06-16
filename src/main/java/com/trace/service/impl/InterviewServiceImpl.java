package com.trace.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trace.dto.InterviewAnswerRequest;
import com.trace.dto.InterviewStartRequest;
import com.trace.entity.InterviewRecord;
import com.trace.mapper.InterviewRecordMapper;
import com.trace.service.InterviewService;
import com.trace.service.PdfService;
import com.trace.service.SearchRouterService;
import com.trace.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final ChatClient.Builder chatClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InterviewRecordMapper recordMapper;
    private final PdfService pdfService;
    private final RabbitTemplate rabbitTemplate;
    private final SearchRouterService searchRouter;

    private static final String SESSION_KEY_PREFIX = "interview:session:";
    private static final int SESSION_TTL_HOURS = 2;
    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 面试教练。你负责出题、评估、给出1-10评分和建设性点评。
            专业、客观、鼓励。""" + "\n\n"
            + "## 出题规则\n"
            + "1. 根据候选人的简历和自我介绍内容，深入提问项目经历、"
            + "技术选型、架构设计、遇到的挑战等\n"
            + "2. 没有候选人背景信息时，根据面试题库出标准化面试题\n"
            + "3. 每道题只输出题目，不要输出编号、解释或其他内容";

    private static final String INTRO_NO_RESUME =
            "欢迎来到我们公司进行面试，请先做一个简单的自我介绍，"
            + "包括你的技术背景和项目经验。";

    private static final String INTRO_HAS_RESUME =
            "欢迎来到我们公司面试，根据你简历上的项目经验，"
            + "请先做一个简单的自我介绍，"
            + "也可以补充简历之外的其他项目经历。";


    @Override
    public Map<String, Object> startInterview(Long userId, InterviewStartRequest request) {
        String sessionId = UUID.randomUUID().toString();

        String resumeText = request.getResumeText() != null
                ? request.getResumeText() : "";

        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("industry", request.getIndustry());
        int totalQ = request.getQuestionCount() != null && request.getQuestionCount() > 0
                ? request.getQuestionCount() : 5;
        session.put("totalQuestions", totalQ);
        session.put("currentQuestion", 0);
        session.put("questions", new ArrayList<Map<String, Object>>());
        session.put("scores", new ArrayList<BigDecimal>());
        session.put("status", "IN_PROGRESS");
        session.put("resumeText", resumeText);
        session.put("selfIntro", "");
        session.put("useKnowledgeBase", request.isUseKnowledgeBase());

        // 第一题：欢迎 + 自我介绍（根据有无简历调整语气）
        String firstQ = resumeText.isBlank()
                ? INTRO_NO_RESUME : INTRO_HAS_RESUME;

        Map<String, Object> fq = new HashMap<>();
        fq.put("question", firstQ);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> qs =
                (List<Map<String, Object>>) session.get("questions");
        qs.add(fq);
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId,
                session, SESSION_TTL_HOURS, TimeUnit.HOURS);
        return Map.of("sessionId", sessionId,
                "question", firstQ,
                "currentQuestion", 1,
                "totalQuestions", totalQ,
                "status", "IN_PROGRESS");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> submitAnswer(String sessionId, InterviewAnswerRequest request) {
        Map<String, Object> s = (Map<String, Object>) redisTemplate.opsForValue().get(SESSION_KEY_PREFIX + sessionId);
        if (s == null) throw new IllegalArgumentException("会话已过期");
        int cur = (int) s.get("currentQuestion"), total = (int) s.get("totalQuestions");
        List<Map<String, Object>> qs = (List<Map<String, Object>>) s.get("questions");
        Map<String, Object> cq = qs.get(cur);
        Map<String, Object> r = new HashMap<>();

        // 第一题是自我介绍 → 保存为后续出题的上下文，不点评直接下一题
        if (cur == 0) {
            s.put("selfIntro", request.getAnswer());
            cq.put("userAnswer", request.getAnswer());
            cur++; s.put("currentQuestion", cur);
            Map<String, Object> nq = generateQuestion(s);
            qs.add(nq);
            r.put("nextQuestion", nq.get("question"));
            r.put("isLast", false);
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId,
                    s, SESSION_TTL_HOURS, TimeUnit.HOURS);
            return r;
        }

        cur++; s.put("currentQuestion", cur);
        if (cur >= total) {
            // 最后一题 → 先存记录，异步生成综合评价
            s.put("status", "COMPLETED");
            r.put("isLast", true);
            cq.put("userAnswer", request.getAnswer());
            BigDecimal avg = new BigDecimal("7");
            Long rid = saveRecord(s, avg);
            r.put("recordId", rid);
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            // RabbitMQ 异步生成评价（携带 Q&A 数据）
            List<Map<String, Object>> allQa = qs.stream()
                    .map(q -> Map.of("question", (Object) q.getOrDefault("question", ""),
                                     "answer", q.getOrDefault("userAnswer", "")))
                    .collect(Collectors.toList());
            Map<String, Object> evalMsg = new HashMap<>();
            evalMsg.put("recordId", rid);
            evalMsg.put("industry", s.get("industry"));
            evalMsg.put("questions", allQa);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.INTERVIEW_EVAL_EXCHANGE,
                    RabbitMQConfig.INTERVIEW_EVAL_ROUTING_KEY,
                    evalMsg);
        } else {
            // 中间题 → 不做点评，直接继续出题
            cq.put("userAnswer", request.getAnswer());
            Map<String, Object> nq = generateQuestion(s);
            qs.add(nq);
            r.put("isLast", false);
            r.put("nextQuestion", nq.get("question"));
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId,
                    s, SESSION_TTL_HOURS, TimeUnit.HOURS);
        }
        return r;
    }

    @Override
    public IPage<InterviewRecord> getRecords(Long userId, int page, int size) {
        List<InterviewRecord> all = recordMapper.findByUserIdOrderByCompletedAtDesc(userId);
        Page<InterviewRecord> mp = new Page<>(page + 1, size);
        int start = (int) mp.offset(), end = Math.min(start + size, all.size());
        mp.setRecords(all.subList(Math.min(start, all.size()), end)); mp.setTotal(all.size());
        return mp;
    }

    @Override
    public InterviewRecord getRecordDetail(Long id) {
        InterviewRecord r = recordMapper.selectById(id);
        if (r == null) throw new IllegalArgumentException("记录不存在");
        return r;
    }

    @Override
    public String generateReport(Long id) {
        InterviewRecord r = getRecordDetail(id);
        String content = r.getAiAnalysis() != null
                ? r.getAiAnalysis()
                : "面试报告生成中...";
        String url = pdfService.generateAndUpload(
                "面试报告_" + r.getIndustry(), content);
        r.setReportUrl(url);
        recordMapper.updateById(r);
        return url;
    }

    private Map<String, Object> generateQuestion(Map<String, Object> session) {
        String industry = (String) session.get("industry");
        String resumeText = (String) session.getOrDefault("resumeText", "");
        String selfIntro = (String) session.getOrDefault("selfIntro", "");
        Long userId = ((Number) session.get("userId")).longValue();
        boolean useKB = (boolean) session.getOrDefault("useKnowledgeBase", false);

        // 搜索面试题库（始终使用）
        List<org.springframework.ai.document.Document> bankDocs =
                searchRouter.interviewSearch(userId, industry, 3);

        // 用户个人知识库（可选）
        List<org.springframework.ai.document.Document> userDocs = List.of();
        if (useKB) {
            userDocs = searchRouter.search(userId, industry,
                    com.trace.enums.SearchType.HYBRID, "USER", 3);
        }

        // 构建上下文
        StringBuilder ctx = new StringBuilder();
        if (resumeText != null && !resumeText.isBlank()) {
            ctx.append("## 候选人简历\n").append(resumeText).append("\n\n");
        }
        if (selfIntro != null && !selfIntro.isBlank()) {
            ctx.append("## 候选人自我介绍\n").append(selfIntro).append("\n\n");
        }
        appendDocs(ctx, "## 面试题库参考", bankDocs);
        if (useKB && !userDocs.isEmpty()) {
            appendDocs(ctx, "## 用户个人知识库参考", userDocs);
        }

        ctx.append("行业：").append(industry)
                .append("\n请根据以上信息出一道面试题。只输出题目。");

        String q = chatClientBuilder.build()
                .prompt().system(SYSTEM_PROMPT)
                .user(ctx.toString())
                .call().content();
        Map<String, Object> m = new HashMap<>();
        m.put("question", q);
        return m;
    }

    private void appendDocs(StringBuilder sb, String title,
                            List<org.springframework.ai.document.Document> docs) {
        if (docs == null || docs.isEmpty()) return;
        sb.append(title).append("\n");
        for (int i = 0; i < Math.min(docs.size(), 3); i++) {
            String content = docs.get(i).getFormattedContent();
            // 截取前 300 字
            if (content.length() > 300) content = content.substring(0, 300) + "...";
            sb.append("- ").append(content).append("\n");
        }
        sb.append("\n");
    }



    private String extractJson(String json, String key) { try { int ki = json.indexOf("\"" + key + "\""); if (ki < 0) return "5"; int ci = json.indexOf(":", ki); int sq = json.indexOf("\"", ci); if (sq < 0) { int s = ci + 1; while (s < json.length() && (Character.isDigit(json.charAt(s)) || json.charAt(s) == '.')) s++; return json.substring(ci + 1, s).trim(); } int eq = json.indexOf("\"", sq + 1); return json.substring(sq + 1, eq); } catch (Exception e) { return "5"; } }

    @SuppressWarnings("unchecked")
    @Transactional
    public Long saveRecord(Map<String, Object> s, BigDecimal avg) {
        InterviewRecord r = InterviewRecord.builder()
                .userId(((Number) s.get("userId")).longValue())
                .industry((String) s.get("industry"))
                .skillTags(List.of())
                .totalQuestions((int) s.get("totalQuestions"))
                .avgScore(avg)
                .completedAt(LocalDateTime.now())
                .build();
        recordMapper.insert(r);
        return r.getId();
    }

    @Override
    public void abortInterview(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        log.info("Interview aborted: sessionId={}", sessionId);
    }

    @Override
    public void deleteRecord(Long id) {
        recordMapper.deleteById(id);
        log.info("Interview record deleted: id={}", id);
    }

    @Override public Flux<String> startInterviewStream(Long userId, InterviewStartRequest req) { Map<String, Object> r = startInterview(userId, req); return Flux.just("{\"sessionId\":\"" + r.get("sessionId") + "\",\"currentQuestion\":1,\"totalQuestions\":" + r.get("totalQuestions") + "}\n" + r.get("question")); }
    @Override
    @SuppressWarnings("unchecked") public Flux<String> submitAnswerStream(String sid, InterviewAnswerRequest req) { Map<String, Object> r = submitAnswer(sid, req);
        return Flux.just(r.toString()); }
}
