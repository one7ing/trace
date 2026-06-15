package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 面试启动请求 —— 行业方向、简历文本、题目数量。
 */
@Data
public class InterviewStartRequest {

    @NotBlank(message = "行业不能为空")
    private String industry;

    /** 简历文本（上传简历后提取的内容），AI 根据简历内容提问 */
    private String resumeText;

    /** 题目数量，默认 20 */
    private Integer questionCount;

    /** 是否加上用户个人知识库出题 */
    private boolean useKnowledgeBase;
}
