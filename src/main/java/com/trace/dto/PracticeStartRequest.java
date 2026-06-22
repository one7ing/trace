package com.trace.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷题启动请求 —— 方向、题目数量、是否结合知识库。
 */
@Data
public class PracticeStartRequest {

    /** 刷题方向（frontend/backend/general 等） */
    @NotBlank(message = "方向不能为空")
    private String topic;

    /** 题目数量，默认 5 */
    @Min(value = 1, message = "题目数量至少为1")
    private Integer questionCount;

    /** 是否结合用户知识库（AI 判题时参考） */
    private boolean useKnowledgeBase;
}
