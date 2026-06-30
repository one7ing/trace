package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.LinkedHashMap;
import java.util.List;

@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    List<LongTermMemory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    int countByUserId(@Param("userId") Long userId);
    void deleteOldestByUserId(@Param("userId") Long userId, @Param("count") int count);

    List<LinkedHashMap<String, Object>> findSimilarMemory(@Param("userId") Long userId,
                                                 @Param("queryVec") String queryVec,
                                                 @Param("threshold") double threshold);
    void updateContentAndEmbedding(@Param("id") Long id, @Param("content") String content, @Param("embedding") String embedding);
    List<LongTermMemory> searchByVector(@Param("userId") Long userId, @Param("queryVec") String queryVec, @Param("limit") int limit);
    void insertembding(@Param("userId") Long userId,@Param("content") String content,@Param("sourceType") String sourceType,@Param("embedding") String embedding);

}
