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

    /**
     * 知识库文档上传主流程。
     * 1. 先往 knowledge_bases 表插入一条记录（用于前端展示），获取 kbId。
     * 2. 将文档内容按语义分块，写入 vector_store 向量表（用于 RAG 检索）。
     * 两个表通过 kbId 关联。
     */
    @Override
    @Transactional
    public KnowledgeBase uploadFile(Long userId, MultipartFile file, String category) {
        // 1. 提取文件名和文本内容
        String fileName = file.getOriginalFilename();
        String text = readFile(file);

        // 3. 先插入知识库元数据行，获得数据库自增主键 kbId
        KnowledgeBase kb = KnowledgeBase.builder()
                .userId(userId)
                .fileName(fileName)
                .category(category)
                .content(text)          // 全量文本，用于详情展示
                .createdAt(LocalDateTime.now())
                .build();
        kbMapper.insert(kb);  // MyBatis-Plus 插入后自动回填 kb.getId()

        // 4. 将长文本切分成多个语义块（每块约 500 token，重叠 100 token），
        //    每个块带上 userId、kbId、category 等元数据，写入向量库（vector_store）
        List<Document> docs = chunkToDocuments(text, userId, kb.getId(), category);
        addToVectorStore(docs);  // 内部调用 embeddingModel 向量化，再批量写入 PgVector

        log.info("KB uploaded: userId={}, kbId={}, file={}, chunks={}", userId, kb.getId(), fileName, docs.size());
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
        if (category != null && !category.isEmpty())
            w.eq(KnowledgeBase::getCategory, category);
        w.orderByDesc(KnowledgeBase::getCreatedAt);
        List<KnowledgeBase> all = kbMapper.selectList(w);
        Page<KnowledgeBase> mpPage = new Page<>(page + 1, size);
        int start = (int) mpPage.offset();
        int end = Math.min(start + size, all.size());
        mpPage.setRecords(all.subList(Math.min(start, all.size()), end));
        mpPage.setTotal(all.size());
        return mpPage;
    }

    // ===== 更新名称（仅改展示表，不动向量）=====

    @Override
    @Transactional
    public void updateName(Long userId, Long id, String newName) {
        KnowledgeBase kb = kbMapper.selectById(id);
        if (kb == null || !kb.getUserId().equals(userId))
            throw new IllegalArgumentException("条目不存在或无权修改");
        kb.setFileName(newName);
        kbMapper.updateById(kb);
    }

    // ===== 删除（基于 kbId 删向量）=====

    @Override
    @Transactional
    public void deleteByFileName(Long userId, String fileName) {
        KnowledgeBase kb = kbMapper.findByFileName(userId, fileName);
        if (kb == null) return;
        // 按 kbId 删向量
        kbMapper.deleteFromVectorStoreByKbId(kb.getId());
        // 删展示行
        kbMapper.deleteById(kb.getId());
    }

    @Override
    @Transactional
    public void clearUserKnowledge(Long userId) {
        // 按 userId 删所有向量
        kbMapper.deleteFromVectorStoreByUserId(userId);
        // 删展示行
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getUserId, userId);
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
        if (kb == null) return;
        // 更新原文
        kb.setContent(newContent);
        kbMapper.updateById(kb);
        // 删除旧向量 + 重新分块向量化
        kbMapper.deleteFromVectorStoreByKbId(kb.getId());
        List<Document> docs = chunkToDocuments(newContent, userId, kb.getId(), kb.getCategory());
        addToVectorStore(docs);
        log.info("KB content updated: kbId={}, chunks={}", kb.getId(), docs.size());
    }

    // ===== 检索 =====

    @Override
    public List<Document> hybridSearch(Long userId, String query, String category, int topK) {
        if (embeddingModel == null) return List.of();
        float[] emb = embeddingModel.embed(query);
        String vecStr = vectorToString(emb);
        String tsQuery = query.replaceAll("[^\\w\\u4e00-\\u9fff]+", " | ")
                .replaceAll("(^\\s*\\|\\s*|\\s*\\|\\s*$)", "").trim();
        if (tsQuery.isEmpty()) tsQuery = query.replaceAll("\\s+", " & ");
        return rowsToDocuments(
                kbMapper.hybridSearch(vecStr, tsQuery, userId, category, topK));
    }

    @Override
    public List<Document> semanticSearch(Long userId, String query, String category) {
        if (embeddingModel == null) return List.of();
        float[] emb = embeddingModel.embed(query);
        return rowsToDocuments(
                kbMapper.semanticSearch(vectorToString(emb), userId, category, 0.4, 4));
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
                    try { metadata = objectMapper.readValue(metaVal.toString(),
                            new TypeReference<Map<String, Object>>() {}); } catch (Exception ignored) {}
                }
                Object sv = row.get("hybrid_score");
                if (sv == null) sv = row.get("similarity");
                docs.add(Document.builder().id(id).text(content).metadata(metadata)
                        .score(sv instanceof Number ? ((Number) sv).doubleValue() : null).build());
            } catch (Exception e) { log.warn("vector_store row 转换失败", e); }
        }
        return docs;
    }

    // ===== 内部：分块（metadata 存 kbId，不存 fileName）=====

    /**
     * 将文本切分为多个语义块（Chunk），用于向量嵌入和检索。
     *
     * @param text     原始文本
     * @param userId   用户ID，存入元数据
     * @param kbId     知识库ID，存入元数据
     * @param category 知识库分类（如 CULTURE、PROFESSIONAL），存入元数据
     * @return 切分后的文档块列表
     */
    private List<Document> chunkToDocuments(String text, Long userId, Long kbId, String category) {
        // 将配置的 token 数估算为字符数（中文约 1 token ≈ 1.5 字符）
        int chunkChars = (int) (CHUNK_TOKENS * TOKEN_TO_CHAR_RATIO);      // 每个块的目标字符数
        int overlapChars = (int) (OVERLAP_TOKENS * TOKEN_TO_CHAR_RATIO);  // 块间重叠字符数

        // --- 第一步：文本拆分为句子列表 ---
        List<String> sentences = new ArrayList<>();
        // 先按空行（两个以上换行）切分为段落
        for (String para : text.split("\\n\\s*\\n")) {
            para = para.trim();
            if (para.isEmpty()) continue;
            // 每个段落内，在句号、问号、感叹号、分号后切开，同时保留标点符号
            for (String s : para.split("(?<=[。！？；])")) {
                s = s.trim();
                if (!s.isEmpty()) sentences.add(s);
            }
        }

        // --- 第二步：动态拼接句子为块，控制大小并添加重叠 ---
        List<Document> docs = new ArrayList<>();
        int globalIdx = 0;  // 全局块序号
        int i = 0;          // 当前起始句子的索引

        while (i < sentences.size()) {
            StringBuilder chunk = new StringBuilder();
            int charCount = 0; // 当前块已累积的字符数
            int j = i;         // 尝试包含的句子索引

            // 在不超出块大小限制的前提下，尽可能加入更多句子
            while (j < sentences.size() && charCount + sentences.get(j).length() <= chunkChars) {
                if (!chunk.isEmpty()) chunk.append(" ");
                chunk.append(sentences.get(j));
                charCount = chunk.length();
                j++;
            }

            // 如果当前块为空（可能遇到一个超长句子），则强制截断它
            if (chunk.isEmpty() && j < sentences.size()) {
                String longSent = sentences.get(j);
                chunk.append(longSent, 0, Math.min(longSent.length(), chunkChars));
                j++;
            }

            // 保存当前块（如果非空）
            String chunkText = chunk.toString().trim();
            if (!chunkText.isEmpty()) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("userId", userId.toString());
                meta.put("kbId", kbId.toString());
                meta.put("category", category);
                meta.put("chunkIndex", String.valueOf(globalIdx++));
                docs.add(new Document(UUID.randomUUID().toString(), chunkText, meta));
            }

            // 如果已经处理完所有句子，结束循环
            if (j >= sentences.size()) break;

            // --- 第三步：计算下一个块的起始位置，实现重叠 ---
            // 重叠逻辑：从当前块的最后一句话向前回溯，累计达到 overlapChars 个字符后，下一个块从那里开始
            int overlapRemaining = overlapChars;  // 需要重叠的剩余字符数
            int k = j - 1;  // 当前块包含的最后一个句子索引
            while (k > i && overlapRemaining > 0) {
                overlapRemaining -= sentences.get(k).length();
                k--;
            }
            // 下一个块的起始索引至少要比当前块多1，且不能小于重叠起点的下一个位置
            i = Math.max(i + 1, k + 1);
            // 防止死循环：如果重叠计算导致起始位置不变或回退，则强制前进到 j
            if (i >= j) i = j;
        }

        return docs;
    }

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

    /**
     * 根据文件扩展名读取并解析文本内容。
     * 支持 TXT、MD（直接读取）和 PDF（使用 iText 提取文本）。
     */
    private String readFile(MultipartFile file) {
        try {
            // 1. 获取原始文件名，用于判断格式
            String name = file.getOriginalFilename();
            if (name == null) throw new IllegalArgumentException("文件名为空");
            String ext = name.toLowerCase();

            // 2. TXT 和 Markdown 文件：直接按 UTF-8 读取
            if (ext.endsWith(".txt") || ext.endsWith(".md"))
                return new String(file.getBytes(), StandardCharsets.UTF_8);

            // 3. PDF 文件：调用专门的 PDF 解析方法
            if (ext.endsWith(".pdf")) return readPdf(file.getBytes());

            // 4. 其他格式暂不支持，抛出异常
            throw new IllegalArgumentException("不支持格式: " + ext);
        } catch (RuntimeException e) {
            throw e;  // 业务异常直接抛出（如格式不支持）
        } catch (Exception e) {
            throw new RuntimeException("读取文件失败", e);  // 其他异常包装为运行时异常
        }
    }

    /**
     * 使用 iText（lowagie 版本）解析 PDF 文件，提取所有页面的纯文本。
     */
    private String readPdf(byte[] data) throws Exception {
        // 创建 PDF 阅读器，从字节数组读取
        com.lowagie.text.pdf.PdfReader reader =
                new com.lowagie.text.pdf.PdfReader(new java.io.ByteArrayInputStream(data));
        // 创建文本提取器
        com.lowagie.text.pdf.parser.PdfTextExtractor extractor =
                new com.lowagie.text.pdf.parser.PdfTextExtractor(reader);
        StringBuilder sb = new StringBuilder();

        // 逐页提取文本，页与页之间用换行分隔
        for (int i = 1; i <= reader.getNumberOfPages(); i++)
            sb.append(extractor.getTextFromPage(i)).append("\n");

        reader.close();
        return sb.toString().trim();  // 去除首尾空白
    }
}
