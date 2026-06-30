package com.trace.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.trace.entity.KnowledgeBase;
import org.springframework.ai.document.Document;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface KnowledgeBaseService {

    /** 上传文件，写入 vector_store + knowledge_bases 展示行 */
    KnowledgeBase uploadFile(Long userId, MultipartFile file, String category);

    /** 按名称模糊搜索 */
    List<KnowledgeBase> search(Long userId, String query, int limit);

    /** 分页查询知识库条目 */
    IPage<KnowledgeBase> listItems(Long userId, String category, int page, int size);

    /** 更新条目名称（同时改文件名 + vector_store metadata） */
    void updateName(Long userId, Long id, String newName);

    /** 按文件名删除（knowledge_bases + vector_store） */
    void deleteByFileName(Long userId, String fileName);

    /** 清空用户知识库 */
    void clearUserKnowledge(Long userId);

    /** 从 vector_store 读取文件全文 */
    String getFileContent(Long userId, String fileName);

    /** 编辑文件内容（重新向量化） */
    void updateFileContent(Long userId, String fileName, String newContent);

    // ===== 检索 =====

    /** 混合检索 vector_store（专业知识用） */
    List<Document> hybridSearch(Long userId, String query, String category, int topK, Long kbId);

    /** 语义检索 vector_store（闲聊用）：topK=4, threshold=0.6 */
    List<Document> semanticSearch(Long userId, String query, String category, Long kbId);
}
