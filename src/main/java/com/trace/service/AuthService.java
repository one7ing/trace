package com.trace.service;

import com.trace.dto.LoginRequest;
import com.trace.dto.RegisterRequest;
import com.trace.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AuthService {

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    /** 上传头像，返回 Base64 URL */
    String updateAvatar(Long userId, MultipartFile file);

    /** 修改用户名，返回新用户名 */
    String updateUsername(Long userId, String newUsername);

    /** 获取用户信息 */
    User getProfile(Long userId);

    /** 重置密码：通过邮箱验证身份后设置新密码 */
    void resetPassword(String email, String newPassword);
}
