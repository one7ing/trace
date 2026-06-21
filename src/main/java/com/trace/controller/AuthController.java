package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.dto.LoginRequest;
import com.trace.dto.RegisterRequest;
import com.trace.entity.User;
import com.trace.mapper.UserMapper;
import com.trace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest r){
        return ResponseEntity
                .ok(ApiResponse
                        .success("注册成功", authService.register(r)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest r){
        return ResponseEntity
                .ok(ApiResponse
                        .success("登录成功", authService.login(r)));
    }

    /** 上传头像（图片文件 → Base64 存储） */
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        User u = userMapper.selectById(userId);
        if (u == null) return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户不存在"));
        try {
            String base64 = "data:" + file.getContentType() + ";base64," +
                    java.util.Base64.getEncoder().encodeToString(file.getBytes());
            u.setAvatarUrl(base64);
            userMapper.updateById(u);
            return ResponseEntity.ok(ApiResponse.success("头像已更新", u.getAvatarUrl()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(500, "上传失败"));
        }
    }

    /** 修改用户名 */
    @PutMapping("/username")
    public ResponseEntity<ApiResponse<String>> updateUsername(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        String newName = body.get("username");
        if (newName == null || newName.trim().length() < 3)
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户名至少3个字符"));
        User u = userMapper.selectById(userId);
        if (u == null) return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户不存在"));
        if (userMapper.existsByUsername(newName) && !u.getUsername().equals(newName))
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "用户名已被占用，请换一个"));
        u.setUsername(newName.trim());
        userMapper.updateById(u);
        return ResponseEntity.ok(ApiResponse.success("用户名已更新", u.getUsername()));
    }

    /** 获取用户信息 */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> profile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userMapper.selectById(userId)));
    }
}
