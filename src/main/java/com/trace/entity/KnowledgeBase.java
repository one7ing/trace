package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * 知识库展示表 —— 仅存储用户可见的条目信息。
 * 实际内容与向量存储在 public.vector_store 中。
 */
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@TableName("knowledge_bases")
public class KnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 文件名（即展示名称） */
    @TableField("file_name")
    private String fileName;

    /** 分类：专业知识问答 / 闲聊问答 */
    private String category;

    /** 原始全文（非切块，用于查看/编辑） */
    private String content;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
