package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InterviewAnswerRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    private String answer;
}
