package com.trace.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trace.dto.InterviewAnswerRequest;
import com.trace.dto.InterviewStartRequest;
import com.trace.entity.InterviewQuestionDetail;
import com.trace.entity.InterviewRecord;
import com.trace.mapper.InterviewQuestionDetailMapper;
import com.trace.mapper.InterviewRecordMapper;
import com.trace.service.InterviewService;
import com.trace.service.MemoryService;
import com.trace.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final ChatClient.Builder chatClientBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final InterviewRecordMapper recordMapper;
    private final InterviewQuestionDetailMapper detailMapper;
    private final MemoryService memoryService;
    private final PdfService pdfService;

    private static final String SESSION_KEY_PREFIX = "interview:session:";
    private static final int SESSION_TTL_HOURS = 2;
    private static final String SYSTEM_PROMPT = """
            你是 Trace 系统的 AI 面试教练。你负责出题、评估、给出1-10评分和建设性点评。专业、客观、鼓励。""";

    @Override
    public Map<String, Object> startInterview(Long userId, InterviewStartRequest request) {
        String sessionId = UUID.randomUUID().toString();

        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("industry", request.getIndustry());
        session.put("skills", request.getSkills());
        session.put("totalQuestions", request.getQuestionCount());
        session.put("currentQuestion", 0);
        session.put("questions", new ArrayList<Map<String, Object>>());
        session.put("scores", new ArrayList<BigDecimal>()); session.put("status", "IN_PROGRESS");

        Map<String, Object> fq = generateQuestion(request.getIndustry(), request.getSkills());
        ((List<Map<String, Object>>) session.get("questions")).add(fq);
        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId, session, SESSION_TTL_HOURS, TimeUnit.HOURS);
        return Map.of("sessionId", sessionId,
                "question", fq.get("question"),
                "currentQuestion", 1,
                "totalQuestions", request.getQuestionCount(),
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
        Map<String, Object> ev = evaluateAnswer((String) s.get("industry"), (List<String>) s.get("skills"), (String) cq.get("question"), request.getAnswer());
        BigDecimal sc = (BigDecimal) ev.get("score");
        ((List<BigDecimal>) s.get("scores")).add(sc);
        cq.put("userAnswer", request.getAnswer()); cq.put("score", sc); cq.put("aiComment", ev.get("comment"));
        Map<String, Object> r = new HashMap<>(); r.put("score", sc); r.put("comment", ev.get("comment"));
        cur++; s.put("currentQuestion", cur);
        if (cur >= total) {
            s.put("status", "COMPLETED"); r.put("isLast", true);
            BigDecimal avg = ((List<BigDecimal>) s.get("scores")).stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(new BigDecimal(((List<BigDecimal>) s.get("scores")).size()), 2, RoundingMode.HALF_UP);
            Long rid = saveRecord(s, avg); r.put("recordId", rid); r.put("avgScore", avg);
            redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
            saveMemory(s, avg);
        } else {
            Map<String, Object> nq = generateQuestion((String) s.get("industry"), (List<String>) s.get("skills"));
            qs.add(nq); r.put("isLast", false); r.put("nextQuestion", nq.get("question"));
            redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId, s, SESSION_TTL_HOURS, TimeUnit.HOURS);
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
        r.setQuestionDetails(detailMapper.findByRecordIdOrderBySequenceNumAsc(id));
        return r;
    }

    @Override
    public String generateReport(Long id) {
        InterviewRecord r = getRecordDetail(id);
        StringBuilder sb = new StringBuilder();
        sb.append("面试行业：").append(r.getIndustry()).append("\n技能：").append(String.join(",", r.getSkillTags())).append("\n平均分：").append(r.getAvgScore()).append("\n\n");
        for (var d : r.getQuestionDetails())
            sb.append("---\n第").append(d.getSequenceNum() + 1).append("题：").append(d.getQuestion()).append("\n回答：").append(d.getUserAnswer()).append("\n点评：").append(d.getAiComment()).append("\n得分：").append(d.getScore()).append("/10\n\n");
        String url = pdfService.generateAndUpload("面试报告_" + r.getIndustry(), sb.toString());
        r.setReportUrl(url); recordMapper.updateById(r);
        return url;
    }

    private Map<String, Object> generateQuestion(String industry, List<String> skills) {
        String q = chatClientBuilder.build()
                .prompt().system(SYSTEM_PROMPT)
                .user(String.format("为%s行业%s出一题。只输出题目。", industry, String.join("、", skills)))
                .call().content();
        Map<String, Object> m = new HashMap<>();
        m.put("question", q);
        return m;
    }

    private Map<String, Object> evaluateAnswer(String industry, List<String> skills, String question, String answer) {
        try {
            String resp = chatClientBuilder.build().prompt()
                    .system(SYSTEM_PROMPT)
                    .user(String.format("评估面试：\n行业：%s\n技能：%s\n题：%s\n答：%s\n输出JSON：{\"score\":8,\"comment\":\"点评\"}", industry, String.join("、", skills), question, answer))
                    .call().content();
            Map<String, Object> m = new HashMap<>();
            m.put("score", new BigDecimal(extractJson(resp, "score")));
            m.put("comment", extractJson(resp, "comment"));
            return m;
        } catch (Exception e) {
            return Map.of("score", new BigDecimal("5"), "comment", "评分暂不可用"); }
    }

    private String extractJson(String json, String key) { try { int ki = json.indexOf("\"" + key + "\""); if (ki < 0) return "5"; int ci = json.indexOf(":", ki); int sq = json.indexOf("\"", ci); if (sq < 0) { int s = ci + 1; while (s < json.length() && (Character.isDigit(json.charAt(s)) || json.charAt(s) == '.')) s++; return json.substring(ci + 1, s).trim(); } int eq = json.indexOf("\"", sq + 1); return json.substring(sq + 1, eq); } catch (Exception e) { return "5"; } }

    @SuppressWarnings("unchecked")
    @Transactional
    public Long saveRecord(Map<String, Object> s, BigDecimal avg) {
        InterviewRecord r = InterviewRecord.builder().userId(((Number) s.get("userId")).longValue()).industry((String) s.get("industry")).skillTags((List<String>) s.get("skills")).totalQuestions((int) s.get("totalQuestions")).avgScore(avg).build();
        recordMapper.insert(r);
        List<Map<String, Object>> qs = (List<Map<String, Object>>) s.get("questions");
        for (int i = 0; i < qs.size(); i++) { Map<String, Object> q = qs.get(i); detailMapper.insert(InterviewQuestionDetail.builder().recordId(r.getId()).question((String) q.get("question")).userAnswer((String) q.getOrDefault("userAnswer", "")).aiComment((String) q.getOrDefault("aiComment", "")).score((BigDecimal) q.getOrDefault("score", BigDecimal.ZERO)).sequenceNum(i).build()); }
        return r.getId();
    }

    @Override public Flux<String> startInterviewStream(Long userId, InterviewStartRequest req) { Map<String, Object> r = startInterview(userId, req); return Flux.just("{\"sessionId\":\"" + r.get("sessionId") + "\",\"currentQuestion\":1,\"totalQuestions\":" + r.get("totalQuestions") + "}\n" + r.get("question")); }
    @Override
    @SuppressWarnings("unchecked") public Flux<String> submitAnswerStream(String sid, InterviewAnswerRequest req) { Map<String, Object> r = submitAnswer(sid, req);
        return Flux.just(r.toString()); }

    @SuppressWarnings("unchecked")
    private void saveMemory(Map<String, Object> s, BigDecimal avg) {
        try { memoryService.saveLongTermMemory(((Number) s.get("userId")).longValue(), String.format("【面试】行业：%s | 技能：%s | 平均分：%s", s.get("industry"), String.join("、", (List<String>) s.get("skills")), avg), "interview", null); } catch (Exception e) { log.error("save memory error", e); }
    }
}
