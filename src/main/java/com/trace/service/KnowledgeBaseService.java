package com.trace.service;

import com.trace.entity.KnowledgeBase;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface KnowledgeBaseService {
    List<KnowledgeBase> uploadFile(Long userId, MultipartFile file, String knowledgeType);
    List<KnowledgeBase> search(Long userId, String query, String knowledgeType, int limit);
    List<KnowledgeBase> list(Long userId, String knowledgeType);
    void deleteById(Long userId, Long id);
    void clearUserKnowledge(Long userId);
    String getFileContent(Long userId, String fileName);
    void updateFileContent(Long userId, String fileName, String newContent);
    void renameFile(Long userId, String oldFileName, String newFileName);
    void deleteByFileName(Long userId, String fileName);

    /**
     * 混合检索（向量相似度 0.7 + 全文相关性 0.3）。
     *
     * @param userId   用户ID
     * @param query    查询文本
     * @param category 元数据过滤（如 interview_question），空表示不过滤
     * @param topK     返回条数
     * @return 排序后的文档列表
     */
    List<org.springframework.ai.document.Document> hybridSearch(Long userId, String query,
                                                                 String category, int topK);
}
