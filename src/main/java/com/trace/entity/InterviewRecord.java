package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("interview_records")
public class InterviewRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String industry;
    @TableField(value = "skill_tags", typeHandler = com.trace.config.ListStringTypeHandler.class)
    private List<String> skillTags;
    @TableField("total_questions")
    private int totalQuestions;
    @TableField("avg_score")
    private BigDecimal avgScore;
    @TableField("report_url")
    private String reportUrl;
    @TableField("ai_analysis")
    private String aiAnalysis;
    @TableField("weak_skills")
    private String weakSkills;
    @TableField(value = "completed_at", fill = FieldFill.INSERT)
    private LocalDateTime completedAt;
    @TableField(exist = false)
    private List<InterviewQuestionDetail> questionDetails;
}
