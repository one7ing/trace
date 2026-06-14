package com.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 会话历史记录 —— 持久化到 PostgreSQL 的聊天消息。
 * <p>
 * 替代 Redis 短期会话上下文，支持按时间分页查询。
 * 每个用户最多保留 500 条。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("chat_history")
public class ChatHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 角色：user 或 ai */
    private String role;

    /** 消息内容 */
    private String content;

    @TableField(value = "created_at", insertStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
}
