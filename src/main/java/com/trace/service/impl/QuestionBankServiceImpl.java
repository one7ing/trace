package com.trace.service.impl;

import com.trace.entity.QuestionBank;
import com.trace.mapper.QuestionBankMapper;
import com.trace.service.QuestionBankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题库服务实现 —— 从题库文件解析并写入 question_bank 表。
 */
@Slf4j
@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    private final QuestionBankMapper questionBankMapper;

    public QuestionBankServiceImpl(QuestionBankMapper questionBankMapper) {
        this.questionBankMapper = questionBankMapper;
    }

    @Override
    @Transactional
    public int importQuestionBank() {
        try {
            String fullText;
            try {
                fullText = Files.readString(Paths.get("题库.txt"), StandardCharsets.UTF_8);
            } catch (IOException e) {
                fullText = Files.readString(Paths.get("src/main/resources/题库.txt"),
                        StandardCharsets.UTF_8);
            }

            // 删除代码块
            fullText = fullText.replaceAll("```[\\s\\S]*?```", "");

            List<Map.Entry<String, String>> qaList = new ArrayList<>();

            // 格式A: "数字. 问题\n候选人：答案"
            Pattern pA = Pattern.compile(
                    "(\\d+)\\.\\s*(.+?)\\n候选人[：:]([\\s\\S]*?)(?=\\n\\d+\\.\\s|\\n(?:一|二|三|四|五|六|七|八|九|十))",
                    Pattern.DOTALL);
            Matcher mA = pA.matcher(fullText);
            while (mA.find()) {
                String q = mA.group(2).trim();
                String a = mA.group(3).trim().replaceAll("^[嗯~！，,\\s]+", "");
                if (q.length() > 3 && a.length() > 10)
                    qaList.add(new AbstractMap.SimpleEntry<>(q, a));
            }
            log.info("Format A 匹配: {}", qaList.size());

            // 格式B: "Q数字：问题\n\n答案"
            Pattern pB = Pattern.compile(
                    "Q\\d+[：:]([^\\n]+)\\n\\n([\\s\\S]+?)(?=\\nQ\\d+[：:]|\\n(?:一|二|三|四|五|六|七|八|九|十)|\\n\\Z)");
            Matcher mB = pB.matcher(fullText);
            int bCount = 0;
            while (mB.find()) {
                String q = mB.group(1).trim();
                String a = mB.group(2).trim().replaceAll("\\n\\n+", "\n");
                if (!qaList.stream().anyMatch(e -> e.getKey().equals(q))
                        && q.length() > 2 && a.length() > 5) {
                    qaList.add(new AbstractMap.SimpleEntry<>(q, a));
                    bCount++;
                }
            }
            log.info("Format B 匹配: {}", bCount);

            // 格式C: 宽松备用
            if (qaList.isEmpty()) {
                String[] lines = fullText.split("\n");
                String currentQ = null;
                StringBuilder currentA = new StringBuilder();
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        if (currentQ != null) currentA.append("\n");
                        continue;
                    }
                    if (line.matches("^[一二三四五六七八九十]、.*")
                            || (line.endsWith("篇") && line.length() <= 10))
                        continue;
                    if (line.matches("^\\d+[\\.\\、].*")
                            || line.matches("^Q\\d+[：:].*")) {
                        if (currentQ != null
                                && currentA.toString().trim().length() > 10) {
                            qaList.add(new AbstractMap.SimpleEntry<>(currentQ,
                                    currentA.toString().trim()));
                        }
                        currentQ = line
                                .replaceFirst("^(\\d+[\\.\\、]|Q\\d+[：:])\\s*", "")
                                .trim();
                        currentA = new StringBuilder();
                    } else if (currentQ != null) {
                        currentA.append(line).append("\n");
                    }
                }
                if (currentQ != null && currentA.toString().trim().length() > 10)
                    qaList.add(new AbstractMap.SimpleEntry<>(currentQ,
                            currentA.toString().trim()));
            }
            log.info("解析原始Q&A总数: {}", qaList.size());

            // 去重
            Set<String> seen = new HashSet<>();
            List<Map.Entry<String, String>> unique = new ArrayList<>();
            for (var qa : qaList) {
                String norm = qa.getKey().replaceAll("\\s+", "").toLowerCase();
                if (seen.add(norm)) unique.add(qa);
            }
            log.info("去重后数量: {}", unique.size());

            // 写入 question_bank 表
            int saved = 0;
            for (var qa : unique) {
                QuestionBank qb = QuestionBank.builder()
                        .topic("general")  // 默认方向，后续可编辑
                        .question(qa.getKey())
                        .referenceAnswer(qa.getValue())
                        .difficulty("medium")
                        .build();
                questionBankMapper.insert(qb);
                saved++;
            }
            log.info("题库导入完成: 共写入{}条", saved);
            return saved;
        } catch (Exception e) {
            log.error("题库导入失败: QuestionBankServiceImpl.importQuestionBank", e);
            throw new RuntimeException("题库导入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<QuestionBank> getRandomQuestions(String topic, int count) {
        if (topic == null || topic.isBlank() || "general".equals(topic)) {
            return questionBankMapper.findRandom(count);
        }
        return questionBankMapper.findRandomByTopic(topic, count);
    }

    @Override
    public List<String> getAllTopics() {
        return questionBankMapper.findAllTopics();
    }
}
