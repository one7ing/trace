package com.trace.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.trace.config.RabbitMQConfig;
import com.trace.dto.PracticeAnswerRequest;
import com.trace.dto.PracticeStartRequest;
import com.trace.entity.PracticeRecord;
import com.trace.entity.QuestionBank;
import com.trace.mapper.PracticeQuestionDetailMapper;
import com.trace.mapper.PracticeRecordMapper;
import com.trace.service.PracticeService;
import com.trace.service.QuestionBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class PracticeServiceImpl implements PracticeService {

    private final QuestionBankService questionBankService;
    private final PracticeRecordMapper recordMapper;
    private final PracticeQuestionDetailMapper detailMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final String SESSION_KEY_PREFIX = "practice:session:";
    private static final int SESSION_TTL_HOURS = 2;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> startPractice(Long userId, PracticeStartRequest request) {
        String sessionId = UUID.randomUUID().toString();

        // 获取题目：bankTopic 优先（用户自建题库），否则从系统题库按 topic 取
        List<QuestionBank> questions;
        String topic;
        if (request.getBankTopic() != null && !request.getBankTopic().isBlank()) {
            topic = request.getBankTopic();
            questions = questionBankService.getRandomFromBank(userId, topic, 100);
        } else {
            topic = request.getTopic() != null ? request.getTopic() : "general";
            questions = questionBankService.getRandomQuestions(topic, 100);
        }
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("题库中暂无该方向的题目，请先导入题库或选择其他方向");
        }

        // 构造题目列表
        List<Map<String, Object>> qList = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            QuestionBank qb = questions.get(i);
            Map<String, Object> qm = new HashMap<>();
            qm.put("sequenceNum", i + 1);
            qm.put("question", qb.getQuestion());
            qm.put("referenceAnswer", qb.getReferenceAnswer());
            qm.put("questionBankId", qb.getId());
            qList.add(qm);
        }

        // 保存会话到 Redis
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("topic", topic);
        session.put("totalQuestions", qList.size());
        session.put("questions", qList);
        session.put("currentQuestion", 1);
        session.put("answers", new HashMap<>());
        session.put("status", "IN_PROGRESS");

        redisTemplate.opsForValue().set(SESSION_KEY_PREFIX + sessionId,
                session, SESSION_TTL_HOURS, TimeUnit.HOURS);

        // 返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", sessionId);
        result.put("totalQuestions", qList.size());
        result.put("status", "IN_PROGRESS");

        // 全部题目列表（不含答案）给前端
        List<Map<String, Object>> preview = new ArrayList<>();
        for (Map<String, Object> q : qList) {
            Map<String, Object> p = new HashMap<>();
            p.put("sequenceNum", q.get("sequenceNum"));
            p.put("question", q.get("question"));
            preview.add(p);
        }
        result.put("questions", preview);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> submitPractice(String sessionId,
                                               PracticeAnswerRequest request) {
        Map<String, Object> s = (Map<String, Object>) redisTemplate.opsForValue()
                .get(SESSION_KEY_PREFIX + sessionId);
        if (s == null) throw new IllegalArgumentException("会话已过期或不存在");

        List<Map<String, Object>> qs = (List<Map<String, Object>>) s.get("questions");
        Map<Integer, String> answers = (Map<Integer, String>) s.get("answers");

        // 接收前端提交的答案（可能只有部分题被回答）
        if (request.getAllAnswers() != null && !request.getAllAnswers().isEmpty()) {
            answers.putAll(request.getAllAnswers());
        }

        if (answers.isEmpty()) {
            throw new IllegalArgumentException("至少回答一道题");
        }

        // 保存记录 + 异步判题（只判有答案的题）
        s.put("status", "COMPLETED");
        Long recordId = saveRecord(s, answers);
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);

        // 只把有答案的题传给 AI
        List<Map<String, Object>> allQa = new ArrayList<>();
        for (Map<String, Object> q : qs) {
            int seq = (int) q.get("sequenceNum");
            String ans = answers.get(seq);
            if (ans == null || ans.isBlank()) continue; // 跳过来作答的
            Map<String, Object> qa = new HashMap<>();
            qa.put("question", q.get("question"));
            qa.put("referenceAnswer", q.getOrDefault("referenceAnswer", ""));
            qa.put("userAnswer", ans);
            qa.put("sequenceNum", seq);
            allQa.add(qa);
        }

        Map<String, Object> judgeMsg = new HashMap<>();
        judgeMsg.put("recordId", recordId);
        judgeMsg.put("topic", s.get("topic"));
        judgeMsg.put("questions", allQa);
        judgeMsg.put("userId", s.get("userId"));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PRACTICE_JUDGE_EXCHANGE,
                RabbitMQConfig.PRACTICE_JUDGE_ROUTING_KEY,
                judgeMsg);

        Map<String, Object> r = new HashMap<>();
        r.put("isLast", true);
        r.put("recordId", recordId);
        r.put("totalQuestions", qs.size());
        return r;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public Long saveRecord(Map<String, Object> s, Map<Integer, String> answers) {
        int total = ((List<Map<String, Object>>) s.get("questions")).size();
        PracticeRecord r = PracticeRecord.builder()
                .userId(((Number) s.get("userId")).longValue())
                .topic((String) s.get("topic"))
                .totalQuestions(total)
                .correctCount(0)
                .score(BigDecimal.ZERO)
                .completedAt(LocalDateTime.now())
                .build();
        recordMapper.insert(r);
        return r.getId();
    }

    @Override
    public IPage<PracticeRecord> getRecords(Long userId, int page, int size) {
        List<PracticeRecord> all = recordMapper
                .findByUserIdOrderByCompletedAtDesc(userId);
        Page<PracticeRecord> mp = new Page<>(page + 1, size);
        int start = (int) mp.offset(), end = Math.min(start + size, all.size());
        mp.setRecords(all.subList(Math.min(start, all.size()), end));
        mp.setTotal(all.size());
        return mp;
    }

    @Override
    public PracticeRecord getRecordDetail(Long id) {
        PracticeRecord r = recordMapper.selectById(id);
        if (r == null) throw new IllegalArgumentException("记录不存在");
        return r;
    }

    @Override
    public void abortPractice(String sessionId) {
        redisTemplate.delete(SESSION_KEY_PREFIX + sessionId);
        log.info("刷题已终止: sessionId={}", sessionId);
    }

    @Override
    public void deleteRecord(Long id) {
        recordMapper.deleteById(id);
        log.info("刷题记录已删除: id={}", id);
    }

    @Override
    public Flux<String> startPracticeStream(Long userId, PracticeStartRequest req) {
        Map<String, Object> r = startPractice(userId, req);
        return Flux.just(r.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Flux<String> submitPracticeStream(String sid, PracticeAnswerRequest req) {
        Map<String, Object> r = submitPractice(sid, req);
        return Flux.just(r.toString());
    }
}
