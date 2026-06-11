package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class InterviewStartRequest {

    @NotBlank(message = "行业不能为空")
    private String industry;

    @NotEmpty(message = "技能不能为空")
    private List<String> skills;

    @Min(value = 1, message = "题目数量至少为1")
    private int questionCount = 5;

    private String difficulty;
}
