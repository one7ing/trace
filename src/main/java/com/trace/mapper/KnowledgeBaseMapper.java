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
     * 返回原始列 Map，由 Service 层手动构造 Document。
     */
    List<java.util.LinkedHashMap<String, Object>> hybridSearch(
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

    /**
     * 直接写入 public.vector_store（替代 Spring AI VectorStore.add）。
     *
     * @param id      Document UUID 字符串
     * @param content 文本内容
     * @param metadata 元数据 JSON 字符串
     * @param vecStr  向量字符串，如 "[0.1,0.2,...]"
     */
    void insertVectorStore(@Param("id") String id,
                           @Param("content") String content,
                           @Param("metadata") String metadata,
                           @Param("vecStr") String vecStr);

    /**
     * 按 ID 列表删除 vector_store 记录。
     */
    void deleteFromVectorStore(@Param("ids") List<String> ids);
}
