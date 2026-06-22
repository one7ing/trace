package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

/**
 * 题库题目实体 —— 用于刷题练习。
 * 数据来源：InterviewBankService 解析题库文件后写入。
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("question_bank")
public class QuestionBank {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 技术方向：frontend / backend / fullstack 等 */
    private String topic;

    /** 题目文本 */
    private String question;

    /** 参考答案 */
    @TableField("reference_answer")
    private String referenceAnswer;

    /** 难度：easy / medium / hard */
    private String difficulty;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private java.time.LocalDateTime createdAt;
}
