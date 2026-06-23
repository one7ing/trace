package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    private String message;

    /** 对话模式：direct / web / rag */
    private String mode;

    /** RAG 模式下指定的知识库名称 */
    private String knowledgeBaseTopic;
}
