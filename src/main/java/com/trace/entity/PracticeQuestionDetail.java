package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * 刷题题目详情 —— 每题的用户答案与 AI 判题结果。
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("practice_question_details")
public class PracticeQuestionDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("record_id")
    private Long recordId;

    /** 题目文本 */
    private String question;

    /** 参考答案 */
    @TableField("reference_answer")
    private String referenceAnswer;

    /** 用户答案 */
    @TableField("user_answer")
    private String userAnswer;

    /** AI 判断是否正确 */
    @TableField("is_correct")
    private Boolean isCorrect;

    /** AI 评分（0-10） */
    private BigDecimal score;

    /** AI 点评 */
    @TableField("ai_comment")
    private String aiComment;

    /** 题目序号 */
    @TableField("sequence_num")
    private int sequenceNum;
}
