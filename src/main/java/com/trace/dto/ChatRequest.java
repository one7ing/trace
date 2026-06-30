package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息不能为空")
    private String message;

    /** 对话模式：direct / web / rag */
    private String mode;

    /** RAG 模式下指定的知识库名称（文件名） */
    private String knowledgeBaseTopic;

    /** RAG 模式下知识库 ID（精准定位，跳过遍历查询） */
    private Long knowledgeBaseId;

    /** RAG 模式下知识库分类：专业知识问答 / 闲聊问答 */
    private String knowledgeBaseCategory;
}
