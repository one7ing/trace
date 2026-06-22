package com.trace.controller;

import com.trace.dto.ApiResponse;
import com.trace.dto.LoginRequest;
import com.trace.dto.RegisterRequest;
import com.trace.entity.User;
import com.trace.mapper.UserMapper;
import com.trace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户认证", description = "用户注册、登录、个人信息管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final UserMapper userMapper;

    @Operation(summary = "用户注册", description = "使用用户名和密码注册新用户，返回 JWT Token")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest r){
        return ResponseEntity
                .ok(ApiResponse
                        .success("注册成功", authService.register(r)));
    }

    @Operation(summary = "用户登录", description = "使用用户名和密码登录，返回 JWT Token（有效期7天）")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest r){
        return ResponseEntity
                .ok(ApiResponse
                        .success("登录成功", authService.login(r)));
    }

    @Operation(summary = "上传头像", description = "上传图片文件，以 Base64 格式存储")
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "图片文件") @RequestParam("file") MultipartFile file) {
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

    @Operation(summary = "修改用户名", description = "修改当前登录用户的用户名")
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

    @Operation(summary = "获取个人信息", description = "获取当前登录用户的个人信息")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> profile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(userMapper.selectById(userId)));
    }
}
