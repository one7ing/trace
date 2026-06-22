package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 刷题记录实体 —— 每次刷题练习的汇总记录。
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("practice_records")
public class PracticeRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 刷题方向 */
    private String topic;

    /** 题目总数 */
    @TableField("total_questions")
    private int totalQuestions;

    /** 答对数量 */
    @TableField("correct_count")
    private int correctCount;

    /** 平均得分（0-10） */
    private java.math.BigDecimal score;

    /** AI 综合评价（Markdown） */
    @TableField("ai_analysis")
    private String aiAnalysis;

    @TableField(value = "completed_at", fill = FieldFill.INSERT)
    private LocalDateTime completedAt;
}
