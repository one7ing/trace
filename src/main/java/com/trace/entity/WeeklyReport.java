package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("weekly_reports")
public class WeeklyReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("week_start")
    private LocalDate weekStart;
    @TableField("week_end")
    private LocalDate weekEnd;
    private String summary;
    @TableField("full_content")
    private String fullContent;
    @TableField("report_url")
    private String reportUrl;
    @TableField(value = "generated_at", fill = FieldFill.INSERT)
    private LocalDateTime generatedAt;
}
