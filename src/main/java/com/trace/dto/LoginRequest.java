package com.trace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** 用户名（与邮箱二选一） */
    private String username;

    /** 邮箱（与用户名二选一，优先使用） */
    private String email;

    @NotBlank(message = "密码不能为空")
    private String password;
}
