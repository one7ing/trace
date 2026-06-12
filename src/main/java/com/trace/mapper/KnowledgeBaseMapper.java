package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库 Mapper —— 常规 CRUD 用 MyBatis-Plus BaseMapper，
 * 向量操作和混合检索在 XML 中定义。
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    /**
     * 按用户和类型查询。
     */
    List<KnowledgeBase> findByUserIdAndType(@Param("userId") Long userId,
                                            @Param("type") String type);

    /**
     * 查询共享知识库（INTERVIEW / WEB）。
     */
    List<KnowledgeBase> findShared();

    /**
     * 删除用户所有知识库。
     */
    int deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 按文件名查询所有分块。
     */
    List<KnowledgeBase> findByFileName(@Param("userId") Long userId,
                                       @Param("fileName") String fileName);

    /**
     * 混合检索：向量相似度(0.7) + 全文 ts_rank(0.3)。
     */
    List<org.springframework.ai.document.Document> hybridSearch(
            @Param("queryVec") String queryVec,
            @Param("tsQuery") String tsQuery,
            @Param("category") String category,
            @Param("topK") int topK);

    /**
     * PgVector 向量插入。
     */
    void insertVector(@Param("id") Long id,
                      @Param("userId") Long userId,
                      @Param("fileName") String fileName,
                      @Param("fileType") String fileType,
                      @Param("content") String content,
                      @Param("vecStr") String vecStr,
                      @Param("knowledgeType") String knowledgeType,
                      @Param("chunkIdx") int chunkIdx);

    /**
     * 向量相似度搜索（全类型）。
     */
    List<KnowledgeBase> similaritySearchAll(@Param("vecStr") String vecStr,
                                            @Param("knowledgeType") String knowledgeType,
                                            @Param("limit") int limit);

    /**
     * 向量相似度搜索（仅用户）。
     */
    List<KnowledgeBase> similaritySearchUser(@Param("userId") Long userId,
                                             @Param("vecStr") String vecStr,
                                             @Param("limit") int limit);
}
