package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlanGenerateRequest {

    @NotBlank(message = "目标不能为空")
    private String goal;
}
