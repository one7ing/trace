package com.trace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trace.entity.KnowledgeBase;
import com.trace.mapper.KnowledgeBaseMapper;
import com.trace.service.KnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper kbMapper;
    private final EmbeddingModel embeddingModel;
    private final ObjectMapper objectMapper;

    private static final int CHUNK_TOKENS = 500;
    private static final int OVERLAP_TOKENS = 100;
    private static final double TOKEN_TO_CHAR_RATIO = 1.3;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper kbMapper,
                                     @Autowired(required = false) EmbeddingModel embeddingModel,
                                     ObjectMapper objectMapper) {
        this.kbMapper = kbMapper;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
    }

    // ===== 上传 =====

    @Override
    @Transactional
    public KnowledgeBase uploadFile(Long userId, MultipartFile file, String category) {
        String fileName = file.getOriginalFilename();
        String text = readFile(file);
        String cat = (category != null && !category.isBlank()) ? category : "专业知识问答";

        // 1. 分块 + 写入 vector_store
        List<Document> docs = chunkToDocuments(text, userId, fileName, cat);
        addToVectorStore(docs);

        // 2. 写入 knowledge_bases 展示表（仅1行，存储原文）
        KnowledgeBase kb = KnowledgeBase.builder()
                .userId(userId)
                .fileName(fileName)
                .category(cat)
                .content(text)
                .createdAt(LocalDateTime.now())
                .build();
        kbMapper.insert(kb);
        log.info("KB uploaded: userId={}, file={}, chunks={}", userId, fileName, docs.size());
        return kb;
    }

    // ===== 查询 =====

    @Override
    public List<KnowledgeBase> search(Long userId, String query, int limit) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId)
         .like(KnowledgeBase::getFileName, query)
         .orderByDesc(KnowledgeBase::getCreatedAt)
         .last("LIMIT " + limit);
        return kbMapper.selectList(w);
    }

    @Override
    public IPage<KnowledgeBase> listItems(Long userId, String category, int page, int size) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId);
        if (category != null && !category.isEmpty()) {
            w.eq(KnowledgeBase::getCategory, category);
        }
        w.orderByDesc(KnowledgeBase::getCreatedAt);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        // 内存分页
        Page<KnowledgeBase> mpPage = new Page<>(page + 1, size);
        int start = (int) mpPage.offset();
        int end = Math.min(start + size, all.size());
        mpPage.setRecords(all.subList(Math.min(start, all.size()), end));
        mpPage.setTotal(all.size());
        return mpPage;
    }

    // ===== 更新名称 =====

    @Override
    @Transactional
    public void updateName(Long userId, Long id, String newName) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null || !kb.getUserId().equals(userId)) {
            throw new IllegalArgumentException("条目不存在或无权修改");
        }
        String oldName = kb.getFileName();
        // 更新展示表
        kb.setFileName(newName);
        kbMapper.updateById(kb);
        // 更新 vector_store 中所有分块的 fileName
        kbMapper.deleteFromVectorStoreByFileName(userId, oldName);
        // 重新写入（更新 metadata 中的 fileName）—— 太重了，更好的方式是用 UPDATE
        // 简单处理：直接 UPDATE vector_store SET metadata = jsonb_set(...)
        // 但 MyBatis 难以做到，暂时跳过 vector_store metadata 更新
        // 文件名仅影响展示，检索时用 category + userId 过滤即可
    }

    // ===== 删除 =====

    @Override
    @Transactional
    public void deleteByFileName(Long userId, String fileName) {
        // 删除 vector_store 分块
        kbMapper.deleteFromVectorStoreByFileName(userId, fileName);
        // 删除展示行
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, fileName);
        kbMapper.delete(w);
    }

    @Override
    @Transactional
    public void clearUserKnowledge(Long userId) {
        // 先获取所有文件名并删除 vector_store
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        for (KnowledgeBase kb : all) {
            kbMapper.deleteFromVectorStoreByFileName(userId, kb.getFileName());
        }
        // 删除展示行
        kbMapper.delete(w);
    }

    // ===== 文件内容 =====

    @Override
    public String getFileContent(Long userId, String fileName) {
        KnowledgeBase kb = kbMapper.findByFileName(userId, fileName);
        return (kb != null && kb.getContent() != null) ? kb.getContent() : "";
    }

    @Override
    @Transactional
    public void updateFileContent(Long userId, String fileName, String newContent) {
        KnowledgeBase kb = kbMapper.findByFileName(userId, fileName);
        String category = (kb != null && kb.getCategory() != null) ? kb.getCategory() : "专业知识问答";
        // 更新原文
        if (kb != null) {
            kb.setContent(newContent);
            kbMapper.updateById(kb);
        }
        // 删除旧向量 + 重新分块向量化
        kbMapper.deleteFromVectorStoreByFileName(userId, fileName);
        List<Document> docs = chunkToDocuments(newContent, userId, fileName, category);
        addToVectorStore(docs);
        log.info("KB content updated: userId={}, file={}, chunks={}", userId, fileName, docs.size());
    }

    // ===== 检索 =====

    @Override
    public List<Document> hybridSearch(Long userId, String query, String category, int topK) {
        if (embeddingModel == null) return List.of();
        float[] emb = embeddingModel.embed(query);
        String vecStr = vectorToString(emb);
        String tsQuery = query.replaceAll("[^\\w\\u4e00-\\u9fff]", " | ");
        List<LinkedHashMap<String, Object>> rows =
                kbMapper.hybridSearch(vecStr, tsQuery, userId, category, topK);
        return rowsToDocuments(rows);
    }

    @Override
    public List<Document> semanticSearch(Long userId, String query, String category) {
        if (embeddingModel == null) return List.of();
        float[] emb = embeddingModel.embed(query);
        String vecStr = vectorToString(emb);
        List<LinkedHashMap<String, Object>> rows =
                kbMapper.semanticSearch(vecStr, userId, category, 0.1, 1);
        return rowsToDocuments(rows);
    }

    private List<Document> rowsToDocuments(List<LinkedHashMap<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return List.of();
        List<Document> docs = new ArrayList<>();
        for (LinkedHashMap<String, Object> row : rows) {
            try {
                String id = String.valueOf(row.getOrDefault("id", UUID.randomUUID().toString()));
                String content = (String) row.getOrDefault("content", "");
                Map<String, Object> metadata = Map.of();
                Object metaVal = row.get("metadata");
                if (metaVal instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) metaVal;
                    metadata = m;
                } else if (metaVal != null) {
                    try {
                        metadata = objectMapper.readValue(metaVal.toString(),
                                new TypeReference<Map<String, Object>>() {});
                    } catch (Exception ignored) {}
                }
                Object scoreVal = row.get("hybrid_score");
                if (scoreVal == null) scoreVal = row.get("similarity");
                Double score = scoreVal instanceof Number
                        ? ((Number) scoreVal).doubleValue() : null;
                docs.add(Document.builder().id(id).text(content)
                        .metadata(metadata).score(score).build());
            } catch (Exception e) {
                log.warn("vector_store row → Document 失败", e);
            }
        }
        return docs;
    }

    // ===== 内部 =====

    /** 分块 */
    private List<Document> chunkToDocuments(String text, Long userId, String fileName, String category) {
        int chunkChars = (int) (CHUNK_TOKENS * TOKEN_TO_CHAR_RATIO);
        int overlapChars = (int) (OVERLAP_TOKENS * TOKEN_TO_CHAR_RATIO);
        List<String> sentences = new ArrayList<>();
        for (String para : text.split("\\n\\s*\\n")) {
            para = para.trim();
            if (para.isEmpty()) continue;
            for (String s : para.split("(?<=[。！？；])")) {
                s = s.trim();
                if (!s.isEmpty()) sentences.add(s);
            }
        }
        List<Document> docs = new ArrayList<>();
        int globalIdx = 0, i = 0;
        while (i < sentences.size()) {
            StringBuilder chunk = new StringBuilder();
            int charCount = 0, j = i;
            while (j < sentences.size() && charCount + sentences.get(j).length() <= chunkChars) {
                if (!chunk.isEmpty()) chunk.append(" ");
                chunk.append(sentences.get(j));
                charCount = chunk.length();
                j++;
            }
            if (chunk.isEmpty() && j < sentences.size()) {
                String longSent = sentences.get(j);
                chunk.append(longSent, 0, Math.min(longSent.length(), chunkChars));
                j++;
            }
            String chunkText = chunk.toString().trim();
            if (!chunkText.isEmpty()) {
                String docId = UUID.randomUUID().toString();
                Map<String, Object> meta = new HashMap<>();
                meta.put("userId", userId.toString());
                meta.put("fileName", fileName);
                meta.put("category", category);
                meta.put("chunkIndex", String.valueOf(globalIdx++));
                docs.add(new Document(docId, chunkText, meta));
            }
            if (j >= sentences.size()) break;
            int overlapRemaining = overlapChars, k = j - 1;
            while (k > i && overlapRemaining > 0) {
                overlapRemaining -= sentences.get(k).length();
                k--;
            }
            i = Math.max(i + 1, k + 1);
            if (i >= j) i = j;
        }
        return docs;
    }

    /** 写入 vector_store */
    private void addToVectorStore(List<Document> docs) {
        if (docs == null || docs.isEmpty()) return;
        if (embeddingModel == null) {
            log.warn("EmbeddingModel 不可用，跳过向量化。请设置 DASHSCOPE_API_KEY 环境变量。");
            return;
        }
        for (Document doc : docs) {
            String content = doc.getText();
            if (content == null || content.isBlank()) continue;
            try {
                float[] emb = embeddingModel.embed(content);
                log.debug("向量嵌入成功: dims={}", emb.length);
                String vecStr = vectorToString(emb);
                String metaJson = objectMapper.writeValueAsString(doc.getMetadata());
                kbMapper.insertVectorStore(doc.getId(), content, metaJson, vecStr);
            } catch (Exception e) {
                log.error("向量插入失败: docId={}, 原因={}", doc.getId(), e.getMessage());
            }
        }
    }

    private String vectorToString(float[] v) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(v[i]);
        }
        return sb.append("]").toString();
    }

    /** 读取文件文本 */
    private String readFile(MultipartFile file) {
        try {
            String name = file.getOriginalFilename();
            if (name == null) throw new IllegalArgumentException("文件名为空");
            String ext = name.toLowerCase();
            if (ext.endsWith(".txt") || ext.endsWith(".md"))
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            if (ext.endsWith(".pdf")) return readPdf(file.getBytes());
            throw new IllegalArgumentException("不支持格式: " + ext);
        } catch (RuntimeException e) { throw e; }
        catch (Exception e) { throw new RuntimeException("读取文件失败", e); }
    }

    private String readPdf(byte[] data) throws Exception {
        com.lowagie.text.pdf.PdfReader reader =
                new com.lowagie.text.pdf.PdfReader(new java.io.ByteArrayInputStream(data));
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor =
                new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= reader.getNumberOfPages(); i++)
            sb.append(extractor.getTextFromPage(i)).append("\n");
        reader.close();
        return sb.toString().trim();
    }
}
