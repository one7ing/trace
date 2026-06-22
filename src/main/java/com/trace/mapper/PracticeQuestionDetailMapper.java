package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.PracticeQuestionDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PracticeQuestionDetailMapper extends BaseMapper<PracticeQuestionDetail> {

    @Select("SELECT * FROM practice_question_details WHERE record_id = #{recordId} ORDER BY sequence_num")
    List<PracticeQuestionDetail> findByRecordId(@Param("recordId") Long recordId);
}
