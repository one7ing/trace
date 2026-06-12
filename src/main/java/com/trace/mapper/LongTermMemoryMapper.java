package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.LongTermMemory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LongTermMemoryMapper extends BaseMapper<LongTermMemory> {

    List<LongTermMemory> findRecentByUserId(@Param("userId") Long userId);


    /** 向量插入 */
    void insertVector(@Param("userId") Long userId,
                      @Param("content") String content,
                      @Param("vecStr") String vecStr,
                      @Param("sourceType") String sourceType,
                      @Param("sourceId") Long sourceId);

    /** 向量相似度检索 */
    List<LongTermMemory> retrieveByVector(@Param("userId") Long userId,
                                          @Param("vecStr") String queryVec,
                                          @Param("limit") int limit);
}
