package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("study_plans")
public class StudyPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String goal;
    @TableField("plan_content")
    private String planContent;
    @TableField("plan_url")
    private String planUrl;
    /** 计划总时长（天），用于打卡进度计算 */
    @TableField("total_duration")
    private Integer totalDuration;
    /** 计划来源：ai=AI生成, manual=用户创建 */
    @TableField("source")
    private String source;
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
