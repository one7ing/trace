package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷题答案提交请求 —— 携带会话ID和本次提交的答案列表。
 */
@Data
public class PracticeAnswerRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /** 当前这道题的答案（逐题模式） */
    private String answer;

    /**
     * 全部模式：所有题目的答案映射。
     * key 为题号（1-based），value 为答案文本。
     */
    private java.util.Map<Integer, String> allAnswers;
}
