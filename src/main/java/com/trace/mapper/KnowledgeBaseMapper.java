package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 知识库 Mapper —— knowledge_bases 展示表 CRUD + vector_store 向量操作。
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /** 删除用户所有知识库 */
    int deleteAllByUserId(@Param("userId") Long userId);

    /** 按文件名查询展示条目 */
    KnowledgeBase findByFileName(@Param("userId") Long userId, @Param("fileName") String fileName);

    /** 混合检索 vector_store（专业知识用） */
    List<java.util.LinkedHashMap<String, Object>> hybridSearch(
            @Param("queryVec") String queryVec,
            @Param("tsQuery") String tsQuery,
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("topK") int topK,
            @Param("kbId") Long kbId);

    /** 语义检索 vector_store（闲聊用）：cosine >= threshold，topK */
    List<java.util.LinkedHashMap<String, Object>> semanticSearch(
            @Param("queryVec") String queryVec,
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("threshold") double threshold,
            @Param("topK") int topK,
            @Param("kbId") Long kbId);

    /** 写入 vector_store */
    void insertVectorStore(@Param("id") String id,
                           @Param("content") String content,
                           @Param("metadata") String metadata,
                           @Param("vecStr") String vecStr);

    /** 按 ID 列表删除 vector_store */
    void deleteFromVectorStore(@Param("ids") List<String> ids);

    /** 按 kbId 删除 vector_store */
    void deleteFromVectorStoreByKbId(@Param("kbId") Long kbId);

    /** 按 userId 删除 vector_store */
    void deleteFromVectorStoreByUserId(@Param("userId") Long userId);
}
