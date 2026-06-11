package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("interview_question_details")
public class InterviewQuestionDetail {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("record_id")
    private Long recordId;
    private String question;
    @TableField("user_answer")
    private String userAnswer;
    @TableField("ai_comment")
    private String aiComment;
    private BigDecimal score;
    @TableField("sequence_num")
    private int sequenceNum;
}
