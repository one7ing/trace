package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("long_term_memories")
public class LongTermMemory {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String content;
    @TableField("source_type")
    private String sourceType;
    @TableField("source_id")
    private Long sourceId;
    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
