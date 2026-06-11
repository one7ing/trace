package com.trace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.trace.entity.InterviewQuestionDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewQuestionDetailMapper extends BaseMapper<InterviewQuestionDetail> {

    @Select("SELECT * FROM interview_question_details WHERE record_id = #{recordId} ORDER BY sequence_num ASC")
    List<InterviewQuestionDetail> findByRecordIdOrderBySequenceNumAsc(Long recordId);
}
