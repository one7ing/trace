package com.trace.service.impl;

import com.trace.entity.QuestionBank;
import com.trace.mapper.QuestionBankMapper;
import com.trace.service.QuestionBankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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

    // ===== 用户自建题库 =====

    /** 创建用户题库并导入题目（文本内容） */
    @Override
    @Transactional
    public Map<String, Object> createBank(Long userId, String name, String content) {
        // 校验名称
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("题库名称不能为空");
        }
        List<QuestionBank> existing = questionBankMapper.findByUserIdAndTopic(userId, name);
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("题库名称已存在");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("题目内容不能为空");
        }
        // 解析文本中的 Q&A
        List<Map.Entry<String, String>> qaList = parseQaFromText(content);
        if (qaList.isEmpty()) {
            throw new IllegalArgumentException("未能从内容中解析出题目，请使用 Q: ... A: ... 格式");
        }
        // 批量写入
        int count = 0;
        for (var qa : qaList) {
            QuestionBank qb = QuestionBank.builder()
                    .topic(name)
                    .question(qa.getKey())
                    .referenceAnswer(qa.getValue() != null ? qa.getValue() : "")
                    .difficulty("medium")
                    .userId(userId)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            questionBankMapper.insert(qb);
            count++;
        }
        log.info("题库创建完成: userId={}, topic={}, count={}", userId, name, count);
        Map<String, Object> result = new HashMap<>();
        result.put("topic", name);
        result.put("count", count);
        return result;
    }

    /** 创建用户题库并导入题目（PDF文件） */
    @Override
    @Transactional
    public Map<String, Object> createBankFromFile(Long userId, String name, MultipartFile file) {
        // 校验名称
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("题库名称不能为空");
        }
        List<QuestionBank> existing = questionBankMapper.findByUserIdAndTopic(userId, name);
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("题库名称已存在");
        }
        // 读取文件内容
        String content;
        try {
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.toLowerCase().endsWith(".pdf")) {
                content = readPdf(file.getBytes());
            } else {
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }
        if (content.isBlank()) {
            throw new IllegalArgumentException("文件内容为空");
        }
        // 解析 Q&A
        List<Map.Entry<String, String>> qaList = parseQaFromText(content);
        if (qaList.isEmpty()) {
            throw new IllegalArgumentException("未能从文件中解析出题目");
        }
        // 批量写入
        int count = 0;
        for (var qa : qaList) {
            QuestionBank qb = QuestionBank.builder()
                    .topic(name)
                    .question(qa.getKey())
                    .referenceAnswer(qa.getValue() != null ? qa.getValue() : "")
                    .difficulty("medium")
                    .userId(userId)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            questionBankMapper.insert(qb);
            count++;
        }
        log.info("题库创建完成(file): userId={}, topic={}, count={}", userId, name, count);
        Map<String, Object> result = new HashMap<>();
        result.put("topic", name);
        result.put("count", count);
        return result;
    }

    /** 解析文本中的 Q&A，支持多种格式 */
    private List<Map.Entry<String, String>> parseQaFromText(String text) {
        List<Map.Entry<String, String>> qaList = new ArrayList<>();

        // 清理代码块
        text = text.replaceAll("```[\\s\\S]*?```", "");

        // 格式A: "Q: 问题\nA: 答案" 或 "问：问题\n答：答案"
        Pattern pQA = Pattern.compile(
                "[Qq问][：:]\s*(.+?)\n[Aa答][：:]\s*([\\s\\S]*?)(?=\n[Qq问][：:]|\n*$)",
                Pattern.DOTALL);
        Matcher mQA = pQA.matcher(text);
        while (mQA.find()) {
            String q = mQA.group(1).trim();
            String a = mQA.group(2).trim();
            if (q.length() > 2 && a.length() > 2) {
                qaList.add(new AbstractMap.SimpleEntry<>(q, a));
            }
        }
        if (!qaList.isEmpty()) {
            log.info("格式 Q:/A: 匹配: {}", qaList.size());
            return dedupQa(qaList);
        }

        // 格式B: "数字. 问题\n答案内容"（如 1. Redis是什么？\nRedis是...）
        Pattern pNum = Pattern.compile(
                "(\\d+)\\.\\s*(.+?)\\n([\\s\\S]*?)(?=\\n\\d+\\.\\s|\\n*$)",
                Pattern.DOTALL);
        Matcher mNum = pNum.matcher(text);
        while (mNum.find()) {
            String q = mNum.group(2).trim();
            String a = mNum.group(3).trim();
            if (q.length() > 3 && a.length() > 5) {
                qaList.add(new AbstractMap.SimpleEntry<>(q, a));
            }
        }
        if (!qaList.isEmpty()) {
            log.info("格式 数字. 匹配: {}", qaList.size());
            return dedupQa(qaList);
        }

        // 格式C: 宽松备用——按行解析，以数字/问号结尾的行作为题目
        String[] lines = text.split("\n");
        String currentQ = null;
        StringBuilder currentA = new StringBuilder();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                if (currentQ != null) currentA.append("\n");
                continue;
            }
            if (line.matches("^\\d+[\\.\\、].*") || line.endsWith("？") || line.endsWith("?")) {
                if (currentQ != null && currentA.toString().trim().length() > 5) {
                    qaList.add(new AbstractMap.SimpleEntry<>(currentQ, currentA.toString().trim()));
                }
                currentQ = line.replaceFirst("^\\d+[\\.\\、]\\s*", "").trim();
                currentA = new StringBuilder();
            } else if (currentQ != null) {
                currentA.append(line).append("\n");
            }
        }
        if (currentQ != null && currentA.toString().trim().length() > 5) {
            qaList.add(new AbstractMap.SimpleEntry<>(currentQ, currentA.toString().trim()));
        }
        log.info("格式 宽松 匹配: {}", qaList.size());
        return dedupQa(qaList);
    }

    /** 去重 */
    private List<Map.Entry<String, String>> dedupQa(List<Map.Entry<String, String>> qaList) {
        Set<String> seen = new HashSet<>();
        List<Map.Entry<String, String>> unique = new ArrayList<>();
        for (var qa : qaList) {
            String norm = qa.getKey().replaceAll("\\s+", "").toLowerCase();
            if (seen.add(norm)) unique.add(qa);
        }
        return unique;
    }

    /** 读取 PDF 文本 */
    private String readPdf(byte[] data) throws Exception {
        com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(
                new java.io.ByteArrayInputStream(data));
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor =
                new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++) {
            sb.append(extractor.getTextFromPage(i)).append("\n");
        }
        reader.close();
        return sb.toString().trim();
    }

    /** 列出用户题库 */
    @Override
    public List<Map<String, Object>> listBanks(Long userId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Map<String, Object>> rows = questionBankMapper.countByUserIdGroupByTopic(userId);
        for (Map<String, Object> row : rows) {
            Map<String, Object> bank = new HashMap<>();
            bank.put("topic", row.get("topic"));
            bank.put("count", row.get("cnt"));
            result.add(bank);
        }
        return result;
    }

    /** 删除用户题库及其下所有题目 */
    @Override
    @Transactional
    public void deleteBank(Long userId, String topic) {
        questionBankMapper.deleteByUserIdAndTopic(userId, topic);
    }

    /** 向题库添加题目 */
    @Override
    @Transactional
    public QuestionBank addQuestion(Long userId, String topic, String question, String referenceAnswer) {
        QuestionBank qb = QuestionBank.builder()
                .topic(topic)
                .question(question)
                .referenceAnswer(referenceAnswer != null ? referenceAnswer : "")
                .difficulty("medium")
                .userId(userId)
                .createdAt(java.time.LocalDateTime.now())
                .build();
        questionBankMapper.insert(qb);
        return qb;
    }

    /** 查看题库题目列表 */
    @Override
    public List<QuestionBank> listQuestions(Long userId, String topic) {
        return questionBankMapper.findByUserIdAndTopic(userId, topic);
    }

    /** 删除单题 */
    @Override
    @Transactional
    public void deleteQuestion(Long userId, Long questionId) {
        QuestionBank qb = questionBankMapper.selectById(questionId);
        if (qb == null) {
            throw new IllegalArgumentException("题目不存在");
        }
        if (!userId.equals(qb.getUserId())) {
            throw new IllegalArgumentException("无权删除");
        }
        questionBankMapper.deleteById(questionId);
    }

    /** 从用户题库随机取题 */
    @Override
    public List<QuestionBank> getRandomFromBank(Long userId, String topic, int count) {
        return questionBankMapper.findRandomByUserIdAndTopic(userId, topic, count);
    }
}
