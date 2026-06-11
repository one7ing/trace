package com.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trace.entity.KnowledgeBase;
import com.trace.mapper.KnowledgeBaseMapper;
import com.trace.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import java.util.Comparator;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper kbMapper, VectorStore vectorStore) {
        this.kbMapper = kbMapper;
        this.vectorStore = vectorStore;
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
        Set<String> docIds = docs.stream().map(Document::getId).collect(Collectors.toSet());
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        if ("USER".equals(knowledgeType))
            w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getKnowledgeType, "USER");
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
        if ("USER".equals(knowledgeType))
            w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getKnowledgeType, "USER");
        else
            w.in(KnowledgeBase::getKnowledgeType, List.of("INTERVIEW", "WEB"));
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

    // ===== 内部 =====
    private List<Document> chunkToDocuments(String text, Long userId, String fileName, String knowledgeType) {
        List<Document> docs = new ArrayList<>();
        int size = 800, overlap = 80, start = 0, idx = 0;
        while (start < text.length()) {
            String chunk = text.substring(start, Math.min(start + size, text.length()));
            String docId = UUID.randomUUID().toString();
            Document doc = new Document(docId, chunk, Map.of("userId", userId.toString(), "fileName", fileName, "knowledgeType", knowledgeType, "chunkIndex", String.valueOf(idx)));
            docs.add(doc);
            start += (size - overlap); idx++;
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
    private String getFileExt(MultipartFile f) {
        String n = f.getOriginalFilename(); if (n == null) return "unknown";
        int d = n.lastIndexOf('.'); return d == -1 ? "unknown" : n.substring(d + 1).toLowerCase();
    }
}
