package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("knowledge_bases")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_type")
    private String fileType;        // pdf / txt / docx

    private String content;         // 原始文本片段

    @TableField("knowledge_type")
    private String knowledgeType;   // USER / INTERVIEW

    @TableField("chunk_index")
    private Integer chunkIndex;     // 片段序号

    private String metadata;        // JSON 元数据（来源、标签等）

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
