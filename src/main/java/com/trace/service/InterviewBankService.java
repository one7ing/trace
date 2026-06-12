package com.trace.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class InterviewBankService {

    private final VectorStore vectorStore;

    public InterviewBankService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public int importQuestionBank() {
        try {
            String fullText;
            try { fullText = Files.readString(Paths.get("题库.txt"), StandardCharsets.UTF_8); }
            catch (IOException e) { fullText = Files.readString(Paths.get("src/main/resources/题库.txt"), StandardCharsets.UTF_8); }

            // 删除代码块
            fullText = fullText.replaceAll("```[\\s\\S]*?```", "");

            List<Map.Entry<String, String>> qaList = new ArrayList<>();

            // 格式A: "数字. 问题\n候选人：答案" （Redis篇等）
            Pattern pA = Pattern.compile("(\\d+)\\.\\s*(.+?)\\n候选人[：:]([\\s\\S]*?)(?=\\n\\d+\\.\\s|\\n(?:一|二|三|四|五|六|七|八|九|十))", Pattern.DOTALL);
            Matcher mA = pA.matcher(fullText);
            while (mA.find()) {
                String q = mA.group(2).trim();
                String a = mA.group(3).trim().replaceAll("^[嗯~！，,\\s]+", "");
                if (q.length() > 3 && a.length() > 10) qaList.add(new AbstractMap.SimpleEntry<>(q, a));
            }
            log.info("Format A matched: {}", qaList.size());

            // 格式B: "Q数字：问题\n\n答案" （JS/HTML/浏览器篇等）
            Pattern pB = Pattern.compile("Q\\d+[：:]([^\\n]+)\\n\\n([\\s\\S]+?)(?=\\nQ\\d+[：:]|\\n(?:一|二|三|四|五|六|七|八|九|十)|\\n\\Z)");
            Matcher mB = pB.matcher(fullText);
            int bCount = 0;
            while (mB.find()) {
                String q = mB.group(1).trim();
                String a = mB.group(2).trim().replaceAll("\\n\\n+", "\n");
                if (!qaList.stream().anyMatch(e -> e.getKey().equals(q)) && q.length() > 2 && a.length() > 5) {
                    qaList.add(new AbstractMap.SimpleEntry<>(q, a));
                    bCount++;
                }
            }
            log.info("Format B matched: {}", bCount);

            // 格式C: 宽松备用：逐行扫描 "数字. xxx" 或 "Q数字：xxx"
            if (qaList.isEmpty()) {
                String[] lines = fullText.split("\n");
                String currentQ = null;
                StringBuilder currentA = new StringBuilder();
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) { if (currentQ != null) currentA.append("\n"); continue; }
                    if (line.matches("^[一二三四五六七八九十]、.*") || (line.endsWith("篇") && line.length() <= 10)) continue;
                    if (line.matches("^\\d+[\\.\\、].*") || line.matches("^Q\\d+[：:].*")) {
                        if (currentQ != null && currentA.toString().trim().length() > 10) {
                            qaList.add(new AbstractMap.SimpleEntry<>(currentQ, currentA.toString().trim()));
                        }
                        currentQ = line.replaceFirst("^(\\d+[\\.\\、]|Q\\d+[：:])\\s*", "").trim();
                        currentA = new StringBuilder();
                    } else if (line.startsWith("候选人") || line.startsWith("面试官")) {
                        String content = line.replaceFirst("^(候选人|面试官)[：:]\\s*", "");
                        if (content.length() > 3) currentA.append(content).append("\n");
                    } else if (currentQ != null) {
                        currentA.append(line).append("\n");
                    }
                }
                if (currentQ != null && currentA.toString().trim().length() > 10)
                    qaList.add(new AbstractMap.SimpleEntry<>(currentQ, currentA.toString().trim()));
            }
            log.info("Total raw Q&A: {}", qaList.size());

            // 去重
            Set<String> seen = new HashSet<>();
            List<Map.Entry<String, String>> unique = new ArrayList<>();
            for (var qa : qaList) {
                String norm = qa.getKey().replaceAll("\\s+", "").toLowerCase();
                if (seen.add(norm)) unique.add(qa);
            }
            log.info("After dedup: {}", unique.size());

            // 分批向量化
            List<Document> docs = new ArrayList<>();
            for (var qa : unique) {
                docs.add(new Document(UUID.randomUUID().toString(),
                    "【问题】" + qa.getKey() + "\n\n【参考答案】" + qa.getValue(),
                    Map.of("knowledgeType", "INTERVIEW", "source", "题库.txt")));
            }
            for (int i = 0; i < docs.size(); i += 10)
                vectorStore.add(docs.subList(i, Math.min(i + 10, docs.size())));

            return unique.size();
        } catch (Exception e) {
            log.error("Import failed", e);
            throw new RuntimeException("题库导入失败: " + e.getMessage(), e);
        }
    }
}
