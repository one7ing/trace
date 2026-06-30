package com.trace.controller;

import com.constant.constant;
import com.trace.dto.ApiResponse;
import com.trace.dto.LoginRequest;
import com.trace.dto.RegisterRequest;
import com.trace.entity.User;
import com.trace.service.AuthService;
import com.trace.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "用户认证", description = "注册、登录、刷新Token、登出、个人资料管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final TokenService tokenService;

    @Operation(summary = "注册")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest r) {
        return ResponseEntity.ok(ApiResponse.success("注册成功", authService.register(r)));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest r) {
        return ResponseEntity.ok(ApiResponse.success("登录成功", authService.login(r)));
    }

    @Operation(summary = "刷新短Token，用长Token换取新的accessToken")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error(constant.Token.CODE_REFRESH_INVALID, "refreshToken不能为空"));
        }
        // 用refreshToken换取新的accessToken
        String newAccessToken = tokenService.refreshAccessToken(refreshToken);
        if (newAccessToken == null) {
            return ResponseEntity.ok(ApiResponse.error(constant.Token.CODE_REFRESH_INVALID, "refreshToken无效或已过期"));
        }
        return ResponseEntity.ok(ApiResponse.success("Token已刷新", Map.of("accessToken", newAccessToken)));
    }

    @Operation(summary = "登出，删除服务端refreshToken")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Long userId) {
        // 删除Redis中的refreshToken
        tokenService.deleteRefreshToken(userId);
        return ResponseEntity.ok(ApiResponse.success("已登出", null));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("头像已更新", authService.updateAvatar(userId, file)));
    }

    @Operation(summary = "修改用户名")
    @PutMapping("/username")
    public ResponseEntity<ApiResponse<String>> updateUsername(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.success("用户名已更新",
                authService.updateUsername(userId, body.get("username"))));
    }

    @Operation(summary = "获取个人信息")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> profile(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(userId)));
    }

    @Operation(summary = "忘记密码：通过邮箱重置密码")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String newPassword = body.get("newPassword");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("邮箱不能为空");
        if (newPassword == null || newPassword.length() < 6) throw new IllegalArgumentException("密码至少6位");
        authService.resetPassword(email, newPassword);
        return ResponseEntity.ok(ApiResponse.success("密码已重置，请使用邮箱登录"));
    }

}
