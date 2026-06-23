package com.trace.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 刷题启动请求 —— 方向、题目数量、是否结合知识库。
 */
@Data
public class PracticeStartRequest {

    /** 刷题方向（frontend/backend/general 等） */
    private String topic;

    /** 用户自建题库名称（与 topic 二选一，bankTopic 优先） */
    private String bankTopic;

    /** 题目数量，默认 5 */
    @Min(value = 1, message = "题目数量至少为1")
    private Integer questionCount;
}
