package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    List<LongTermMemory> findRecentByUserId(@Param("userId") Long userId, @Param("limit") int limit);
    int countByUserId(@Param("userId") Long userId);
    void deleteOldestByUserId(@Param("userId") Long userId, @Param("count") int count);

    List<Map<String, Object>> findSimilarMemory(@Param("userId") Long userId,
                                                 @Param("queryVec") String queryVec,
                                                 @Param("threshold") double threshold);
    void updateContentAndEmbedding(@Param("id") Long id, @Param("content") String content, @Param("embedding") String embedding);
    List<Long> findLeastRelevantIds(@Param("userId") Long userId, @Param("count") int count);
    List<LongTermMemory> searchByVector(@Param("userId") Long userId, @Param("queryVec") String queryVec, @Param("limit") int limit);
}
