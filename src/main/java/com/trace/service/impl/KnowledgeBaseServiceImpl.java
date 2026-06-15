package com.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trace.entity.KnowledgeBase;
import com.trace.mapper.KnowledgeBaseMapper;
import com.trace.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Comparator;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper kbMapper,
                                     VectorStore vectorStore,
                                     @Autowired(required = false) EmbeddingModel embeddingModel) {
        this.kbMapper = kbMapper;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    @Override
    @Transactional
    public List<KnowledgeBase> uploadFile(Long userId, MultipartFile file, String knowledgeType) {
        String text = readFile(file);
        List<Document> docs = chunkToDocuments(text, userId, file.getOriginalFilename(), knowledgeType);
        // DashScope embedding 批量限制 ≤10，分批添加
        for (int i = 0; i < docs.size(); i += 10) {
            int end = Math.min(i + 10, docs.size());
            vectorStore.add(docs.subList(i, end));
        }
        List<KnowledgeBase> results = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeBase kb = KnowledgeBase.builder()
                    .userId(userId).fileName(file.getOriginalFilename())
                    .fileType(getFileExt(file)).content(docs.get(i).getFormattedContent())
                    .knowledgeType(knowledgeType).chunkIndex(i)
                    .metadata("{\"docId\":\"" + docs.get(i).getId() + "\"}").build();
            kbMapper.insert(kb);
            results.add(kb);
        }
        log.info("KB uploaded: userId={}, file={}, chunks={}", userId, file.getOriginalFilename(), docs.size());
        return results;
    }

    @Override
    public List<KnowledgeBase> search(Long userId, String query, String knowledgeType, int limit) {
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(limit).build());
        if (docs.isEmpty()) return List.of();
        Set<String> docIds = docs.stream()
                .map(Document::getId)
                .collect(Collectors.toSet());
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        if ("USER".equals(knowledgeType))
            w.eq(KnowledgeBase::getUserId, userId)
                    .eq(KnowledgeBase::getKnowledgeType, "USER");
        else
            w.in(KnowledgeBase::getKnowledgeType, List.of("INTERVIEW", "WEB"));
        List<KnowledgeBase> matched = kbMapper.selectList(w).stream()
                .filter(kb -> docIds.contains(extractDocId(kb.getMetadata())))
                .collect(Collectors.toList());
        // 按文件名去重
        Map<String, KnowledgeBase> grouped = new LinkedHashMap<>();
        for (KnowledgeBase kb : matched) {
            String key = kb.getFileName() + "_" + kb.getUserId();
            if (!grouped.containsKey(key)) grouped.put(key, kb);
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    public List<KnowledgeBase> list(Long userId, String knowledgeType) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId);
        if (knowledgeType != null) {
            w.eq(KnowledgeBase::getKnowledgeType, knowledgeType);
        }
        w.orderByDesc(KnowledgeBase::getCreatedAt);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        // 按文件名去重，每个文件只返回一条
        Map<String, KnowledgeBase> grouped = new LinkedHashMap<>();
        for (KnowledgeBase kb : all) {
            String key = kb.getFileName() + "_" + kb.getUserId();
            if (!grouped.containsKey(key)) grouped.put(key, kb);
        }
        return new ArrayList<>(grouped.values());
    }

    @Override @Transactional
    public void deleteById(Long userId, Long id) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null) throw new IllegalArgumentException("不存在");
        if (!kb.getUserId().equals(userId)) throw new IllegalArgumentException("无权删除");
        vectorStore.delete(List.of(extractDocId(kb.getMetadata())));
        kbMapper.deleteById(id);
    }

    @Override @Transactional
    public void clearUserKnowledge(Long userId) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getKnowledgeType, "USER");
        List<KnowledgeBase> all = kbMapper.selectList(w);
        for (KnowledgeBase kb : all) vectorStore.delete(List.of(extractDocId(kb.getMetadata())));
        kbMapper.delete(w);
    }

    @Override
    public String getFileContent(Long userId, String fileName) {
        List<KnowledgeBase> chunks = kbMapper.findByFileName(userId, fileName);
        if (chunks.isEmpty()) throw new IllegalArgumentException("文件不存在");
        return chunks.stream()
                .sorted(Comparator.comparingInt(KnowledgeBase::getChunkIndex))
                .map(KnowledgeBase::getContent)
                .collect(Collectors.joining("\n"));
    }

    @Override @Transactional
    public void updateFileContent(Long userId, String fileName, String newContent) {
        // 删除旧分块
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, fileName);
        List<KnowledgeBase> old = kbMapper.selectList(w);
        for (KnowledgeBase kb : old) {
            try { vectorStore.delete(List.of(extractDocId(kb.getMetadata()))); } catch (Exception ignored) {}
        }
        kbMapper.delete(w);

        // 重新分块 + 嵌入
        List<Document> docs = chunkToDocuments(newContent, userId, fileName, "USER");
        for (int i = 0; i < docs.size(); i += 10) {
            int end = Math.min(i + 10, docs.size());
            vectorStore.add(docs.subList(i, end));
        }
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeBase kb = KnowledgeBase.builder()
                    .userId(userId).fileName(fileName)
                    .fileType(getFileExtFromName(fileName))
                    .content(docs.get(i).getFormattedContent())
                    .knowledgeType("USER").chunkIndex(i)
                    .metadata("{\"docId\":\"" + docs.get(i).getId() + "\"}").build();
            kbMapper.insert(kb);
        }
    }

    @Override @Transactional
    public void deleteByFileName(Long userId, String fileName) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, fileName);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        for (KnowledgeBase kb : all) {
            try { vectorStore.delete(List.of(extractDocId(kb.getMetadata()))); } catch (Exception ignored) {}
        }
        kbMapper.delete(w);
    }

    @Override @Transactional
    public void renameFile(Long userId, String oldName, String newName) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, oldName);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        for (KnowledgeBase kb : all) {
            kb.setFileName(newName);
            kbMapper.updateById(kb);
        }
    }

    @Override
    public List<Document> hybridSearch(Long userId, String query, String category, int topK) {
        if (embeddingModel == null) {
            return List.of();
        }
        float[] emb = embeddingModel.embed(query);
        String vecStr = vectorToString(emb);
        String tsQuery = query.replaceAll("[^\\w\\u4e00-\\u9fff]", " | ");
        return kbMapper.hybridSearch(vecStr, tsQuery, category, topK);
    }

    // ===== 内部 =====
    /**
     * 语义分块：按句号、换行、段落分隔符切分，保证每块是完整语义单元。
     */
    private List<Document> chunkToDocuments(String text, Long userId, String fileName,
                                            String knowledgeType) {
        List<Document> docs = new ArrayList<>();
        // 先按段落分隔（两个以上换行）
        String[] paragraphs = text.split("\\n\\s*\\n");
        int globalIdx = 0;
        StringBuilder currentChunk = new StringBuilder();

        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) {
                continue;
            }
            // 段落内按句号/问号/感叹号/分号切分句子，保留标点
            String[] sentences = para.split("(?<=[。！？；])");
            for (String sentence : sentences) {
                sentence = sentence.trim();
                if (sentence.isEmpty()) {
                    continue;
                }
                // 当前块 + 新句子超过 800 字且当前块已有内容 → 提交当前块
                if (currentChunk.length() > 0
                        && currentChunk.length() + sentence.length() > 800) {
                    String chunkText = currentChunk.toString().trim();
                    if (!chunkText.isEmpty()) {
                        String docId = UUID.randomUUID().toString();
                        docs.add(new Document(docId, chunkText,
                                Map.of("userId", userId.toString(),
                                        "fileName", fileName,
                                        "knowledgeType", knowledgeType,
                                        "chunkIndex", String.valueOf(globalIdx++))));
                    }
                    currentChunk.setLength(0);
                }
                // 单句超过 800 字，截断
                if (sentence.length() > 800 && currentChunk.length() == 0) {
                    for (int i = 0; i < sentence.length(); i += 750) {
                        String sub = sentence.substring(i,
                                Math.min(i + 750, sentence.length()));
                        String docId = UUID.randomUUID().toString();
                        docs.add(new Document(docId, sub,
                                Map.of("userId", userId.toString(),
                                        "fileName", fileName,
                                        "knowledgeType", knowledgeType,
                                        "chunkIndex", String.valueOf(globalIdx++))));
                    }
                } else {
                    if (currentChunk.length() > 0) {
                        currentChunk.append(" ");
                    }
                    currentChunk.append(sentence);
                }
            }
            // 段落结束，提交当前块
            if (currentChunk.length() > 0) {
                String chunkText = currentChunk.toString().trim();
                if (!chunkText.isEmpty()) {
                    String docId = UUID.randomUUID().toString();
                    docs.add(new Document(docId, chunkText,
                            Map.of("userId", userId.toString(),
                                    "fileName", fileName,
                                    "knowledgeType", knowledgeType,
                                    "chunkIndex", String.valueOf(globalIdx++))));
                }
                currentChunk.setLength(0);
            }
        }
        // 剩余内容
        if (currentChunk.length() > 0) {
            String chunkText = currentChunk.toString().trim();
            if (!chunkText.isEmpty()) {
                String docId = UUID.randomUUID().toString();
                docs.add(new Document(docId, chunkText,
                        Map.of("userId", userId.toString(),
                                "fileName", fileName,
                                "knowledgeType", knowledgeType,
                                "chunkIndex", String.valueOf(globalIdx))));
            }
        }
        return docs;
    }

    private String extractDocId(String metadata) {
        try { return metadata.replaceAll(".*\"docId\":\"([^\"]+)\".*", "$1"); } catch (Exception e) { return ""; }
    }

    private String readFile(MultipartFile file) {
        try {
            String name = file.getOriginalFilename(); if (name == null) throw new IllegalArgumentException("文件名为空");
            String ext = name.toLowerCase();
            if (ext.endsWith(".txt") || ext.endsWith(".md")) return new String(file.getBytes(), StandardCharsets.UTF_8);
            if (ext.endsWith(".pdf")) return readPdf(file.getBytes());
            throw new IllegalArgumentException("不支持格式: " + ext + "，支持 PDF/TXT/MD");
        } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new RuntimeException(e.getMessage()); }
    }

    private String readPdf(byte[] data) throws Exception {
        com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(new java.io.ByteArrayInputStream(data)));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= pdf.getNumberOfPages(); i++)
            sb.append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(pdf.getPage(i))).append("\n");
        pdf.close();
        return sb.toString().trim();
    }

    private String getFileExtFromName(String name) { int d = name.lastIndexOf('.'); return d == -1 ? "unknown" : name.substring(d + 1).toLowerCase(); }
    /**
     * 将 float[] 向量转为 PostgreSQL vector 字符串格式。
     */
    private String vectorToString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }

    private String getFileExt(MultipartFile f) {
        String n = f.getOriginalFilename(); if (n == null) return "unknown";
        int d = n.lastIndexOf('.'); return d == -1 ? "unknown" : n.substring(d + 1).toLowerCase();
    }
}
