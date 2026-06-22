package com.trace.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public KnowledgeBaseServiceImpl(KnowledgeBaseMapper kbMapper,
                                     VectorStore vectorStore,
                                     @Autowired(required = false) EmbeddingModel embeddingModel,
                                     ObjectMapper objectMapper) {
        this.kbMapper = kbMapper;
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public List<KnowledgeBase> uploadFile(Long userId, MultipartFile file, String knowledgeType) {
        String text = readFile(file);
        List<Document> docs = chunkToDocuments(text, userId, file.getOriginalFilename(), knowledgeType);
        addToVectorStore(docs);
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
            w.eq(KnowledgeBase::getKnowledgeType, "INTERVIEW");
        List<KnowledgeBase> matched = kbMapper.selectList(w).stream()
                .filter(kb -> docIds.contains(extractDocId(kb.getMetadata())))
                .toList();
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

    @Override
    @Transactional
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

    @Override
    @Transactional
    public void updateFileContent(Long userId, String fileName, String newContent) {
        // 删除旧分块
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, fileName);
        List<KnowledgeBase> old = kbMapper.selectList(w);
        for (KnowledgeBase kb : old) {
            try { deleteFromVectorStoreByIds(List.of(extractDocId(kb.getMetadata()))); } catch (Exception ignored) {}
        }
        kbMapper.delete(w);

        // 重新分块 + 嵌入
        List<Document> docs = chunkToDocuments(newContent, userId, fileName, "USER");
        for (int i = 0; i < docs.size(); i += 10) {
            int end = Math.min(i + 10, docs.size());
            addToVectorStore(docs.subList(i, end));
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

    @Override
    @Transactional
    public void deleteByFileName(Long userId, String fileName) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, fileName);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        for (KnowledgeBase kb : all) {
            try { vectorStore.delete(List.of(extractDocId(kb.getMetadata()))); } catch (Exception ignored) {}
        }
        kbMapper.delete(w);
    }

    @Override
    @Transactional
    public void renameFile(Long userId, String oldName, String newName) {
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId).eq(KnowledgeBase::getFileName, oldName);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        for (KnowledgeBase kb : all) {
            kb.setFileName(newName);
            kbMapper.updateById(kb);
        }
    }

    /**
     * 执行混合检索（向量语义相似度 + 关键词全文检索加权融合）
     *
     * @param userId   当前用户ID，用于权限过滤（该参数在此方法中未直接使用，由 Mapper SQL 通过 metadata 过滤）
     * @param query    用户输入的原始查询文本
     * @param topK     返回最相关文档的数量
     * @return 混合检索后排序的文档列表，如果无法检索则返回空列表
     */
    @Override
    public List<Document> hybridSearch(Long userId, String query, String category, int topK) {
        // 嵌入模型未初始化时直接返回空，防止空指针
        if (embeddingModel == null) {
            return List.of();
        }

        // ===== 1. 构造查询向量与全文检索字符串 =====
        // 调用阿里云 text-embedding-v3 生成 1024 维查询向量
        float[] emb = embeddingModel.embed(query);
        // 将向量数组转为 PostgreSQL 支持的字符串格式，如 '[0.1,0.2,...]'
        String vecStr = vectorToString(emb);
        // 将原始查询中的非字母/数字/中文替换为 " | "，构造 OR 语法给 tsquery
        String tsQuery = query.replaceAll("[^\\w\\u4e00-\\u9fff]", " | ");

        // ===== 2. 执行混合检索 SQL =====
        // 调用 MyBatis Mapper，传入向量、tsQuery、分类、用途、topK
        // 返回原始行数据，每行为 LinkedHashMap（保持列顺序）
        List<LinkedHashMap<String, Object>> rawRows =
                kbMapper.hybridSearch(vecStr, tsQuery, category, topK);
        // 无结果直接返回空列表
        if (rawRows == null || rawRows.isEmpty()) {
            return List.of();
        }

        // ===== 3. 将原始行转换为 Spring AI Document 对象 =====
        List<Document> docs = new ArrayList<>();
        for (LinkedHashMap<String, Object> row : rawRows) {
            try {
                // 提取文档ID，若数据库未提供则生成 UUID
                String id = String.valueOf(row.getOrDefault("id", UUID.randomUUID().toString()));
                // 提取文本内容，默认空串
                String content = (String) row.getOrDefault("content", "");

                // ----- 解析 metadata 字段 -----
                // PostgreSQL 的 JSONB 字段可能被 JDBC 驱动映射为 PGobject 或 Map，需要兼容处理
                Map<String, Object> metadata = Map.of();
                Object metaVal = row.get("metadata");
                if (metaVal != null) {
                    // 如果驱动直接解析为 Map（例如使用 MyBatis-Plus 的 JSON 处理器）
                    if (metaVal instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> m = (Map<String, Object>) metaVal;
                        metadata = m;
                    } else {
                        // 否则可能为 PGobject 或 String，需要手动解析 JSON
                        String metaJson = metaVal.toString();
                        if (metaJson != null && !metaJson.isBlank() && !"{}".equals(metaJson)) {
                            metadata = objectMapper.readValue(metaJson,
                                    new TypeReference<Map<String, Object>>() {});
                        }
                    }
                }

                // ----- 提取混合检索相关性分数 -----
                Object scoreVal = row.get("hybrid_score");
                // 如果分数为 Number 类型，转为 double；否则设为 null
                Double score = scoreVal instanceof Number
                        ? Double.valueOf(((Number) scoreVal).doubleValue())
                        : null;

                // 构建 Spring AI 的 Document 对象
                Document doc = Document.builder()
                        .id(id)            // 文档唯一ID
                        .text(content)     // 文本内容
                        .metadata(metadata)// 元数据（包含 knowledgeType, usage, source 等）
                        .score(score)      // 混合检索最终分数，用于排序或阈值判断
                        .build();
                docs.add(doc);
            } catch (Exception e) {
                // 单行解析失败不影响其他行，记录日志并跳过
                log.warn("混合检索行转换为Document失败: KnowledgeBaseServiceImpl.hybridSearchToDocuments, row={}", row, e);
            }
        }
        return docs;
    }

    // ===== 内部 =====

    /** 每块目标 token 数 */
    private static final int CHUNK_TOKENS = 500;
    /** 块间重叠 token 数 */
    private static final int OVERLAP_TOKENS = 100;
    /** 中文约 1 token ≈ 1.5 字符，取保守值 1.3 */
    private static final double TOKEN_TO_CHAR_RATIO = 1.3;

    /**
     * Token 级滑动窗口切块：每块约 500 token，重叠 100 token。
     * <p>
     * 先按段落/句子拆分，然后以滑动窗口方式组装 chunk，
     * 确保每块在句子边界断开，相邻块共享约 100 token 的上下文。
     * </p>
     */
    private List<Document> chunkToDocuments(String text, Long userId, String fileName,
                                            String knowledgeType) {
        int chunkChars = (int) (CHUNK_TOKENS * TOKEN_TO_CHAR_RATIO);   // ≈650 chars
        int overlapChars = (int) (OVERLAP_TOKENS * TOKEN_TO_CHAR_RATIO); // ≈130 chars

        // 1. 拆成句子列表
        List<String> sentences = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        for (String para : paragraphs) {
            para = para.trim();
            if (para.isEmpty()) continue;
            // 按句末标点切分，保留标点
            String[] parts = para.split("(?<=[。！？；])");
            for (String s : parts) {
                s = s.trim();
                if (!s.isEmpty()) sentences.add(s);
            }
        }

        // 2. 滑动窗口组装 chunk
        List<Document> docs = new ArrayList<>();
        int globalIdx = 0;
        int i = 0;
        while (i < sentences.size()) {
            StringBuilder chunk = new StringBuilder();
            int charCount = 0;
            int j = i;
            // 正向填充到 chunkChars 上限
            while (j < sentences.size() && charCount + sentences.get(j).length() <= chunkChars) {
                if (!chunk.isEmpty()) chunk.append(" ");
                chunk.append(sentences.get(j));
                charCount = chunk.length();
                j++;
            }
            // 如果单句就超长，强制放入（截断）
            if (chunk.isEmpty() && j < sentences.size()) {
                String longSent = sentences.get(j);
                chunk.append(longSent, 0, Math.min(longSent.length(), chunkChars));
                j++;
            }

            String chunkText = chunk.toString().trim();
            if (!chunkText.isEmpty()) {
                String docId = UUID.randomUUID().toString();
                docs.add(new Document(docId, chunkText,
                        buildMetadata(userId, fileName, knowledgeType, globalIdx++)));
            }

            // 计算下一个窗口起点：回退 overlapChars 个字符
            if (j >= sentences.size()) break; // 已到末尾

            int overlapRemaining = overlapChars;
            int k = j - 1;
            while (k > i && overlapRemaining > 0) {
                overlapRemaining -= sentences.get(k).length();
                k--;
            }
            i = Math.max(i + 1, k + 1);
            // 防止死循环：如果 i 没有前进，强制前进
            if (i >= j) i = j;
        }

        return docs;
    }

    private Map<String, Object> buildMetadata(Long userId, String fileName,
                                               String knowledgeType, int chunkIdx) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("userId", userId.toString());
        meta.put("fileName", fileName);
        meta.put("knowledgeType", knowledgeType);
        meta.put("chunkIndex", String.valueOf(chunkIdx));
        return meta;
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
        } catch (RuntimeException e) { throw e; } catch (Exception e) { throw new RuntimeException("KnowledgeBaseServiceImpl.readFile读取文件失败: " + e.getMessage(), e); }
    }

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

    // ===== vector_store 直接写入（替代 Spring AI VectorStore） =====

    /**
     * 将 Document 列表写入 public.vector_store。
     * 手动调用 EmbeddingModel 生成向量 + MyBatis insert。
     */
    private void addToVectorStore(List<Document> docs) {
        if (docs == null || docs.isEmpty() || embeddingModel == null) {
            return;
        }
        for (Document doc : docs) {
            String content = doc.getText();
            if (content == null || content.isBlank()) {
                continue;
            }
            try {
                float[] emb = embeddingModel.embed(content);
                String vecStr = vectorToString(emb);
                String metaJson = objectMapper.writeValueAsString(doc.getMetadata());
                kbMapper.insertVectorStore(doc.getId(), content, metaJson, vecStr);
            } catch (Exception e) {
                log.error("知识库向量插入失败: docId={}, 错误位置=KnowledgeBaseServiceImpl.insertVectors", doc.getId(), e);
            }
        }
    }

    /**
     * 按 ID 删除 public.vector_store 记录。
     */
    private void deleteFromVectorStoreByIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        kbMapper.deleteFromVectorStore(ids);
    }
}
