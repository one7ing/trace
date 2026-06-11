package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.KnowledgeBase;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

    @Select("SELECT * FROM knowledge_bases WHERE user_id = #{userId} AND knowledge_type = #{type} ORDER BY created_at DESC")
    List<KnowledgeBase> findByUserIdAndType(Long userId, String type);

    @Select("SELECT * FROM knowledge_bases WHERE knowledge_type IN ('INTERVIEW','WEB') ORDER BY created_at DESC")
    List<KnowledgeBase> findShared();

    @Delete("DELETE FROM knowledge_bases WHERE user_id = #{userId} AND knowledge_type = 'USER'")
    int deleteAllByUserId(Long userId);

    /** PgVector 向量插入 */
    @Insert("INSERT INTO knowledge_bases (id,user_id,file_name,file_type,content,embedding,knowledge_type,chunk_index,metadata,created_at) VALUES (#{id},#{userId},#{fileName},#{fileType},#{content},#{vecStr}::vector,#{knowledgeType},#{chunkIdx},'{}'::jsonb,CURRENT_TIMESTAMP)")
    void insertVector(@Param("id") Long id, @Param("userId") Long userId, @Param("fileName") String fileName,
                      @Param("fileType") String fileType, @Param("content") String content,
                      @Param("vecStr") String vecStr, @Param("knowledgeType") String knowledgeType,
                      @Param("chunkIdx") int chunkIdx);

    /** PgVector <=> 余弦相似度搜索 */
    @Select("SELECT * FROM knowledge_bases WHERE knowledge_type = #{knowledgeType} ORDER BY embedding <=> #{vecStr}::vector LIMIT #{limit}")
    List<KnowledgeBase> similaritySearchAll(@Param("vecStr") String vecStr, @Param("knowledgeType") String knowledgeType, @Param("limit") int limit);

    @Select("SELECT * FROM knowledge_bases WHERE user_id = #{userId} AND knowledge_type = 'USER' ORDER BY embedding <=> #{vecStr}::vector LIMIT #{limit}")
    List<KnowledgeBase> similaritySearchUser(@Param("userId") Long userId, @Param("vecStr") String vecStr, @Param("limit") int limit);

    /** 获取某个文件的所有分块 */
    @Select("SELECT * FROM knowledge_bases WHERE user_id = #{userId} AND file_name = #{fileName} ORDER BY chunk_index ASC")
    List<KnowledgeBase> findByFileName(@Param("userId") Long userId, @Param("fileName") String fileName);
}
