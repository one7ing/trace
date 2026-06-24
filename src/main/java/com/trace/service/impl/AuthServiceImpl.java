package com.trace.service.impl;

import com.trace.dto.LoginRequest;
import com.trace.dto.RegisterRequest;
import com.trace.entity.User;
import com.trace.mapper.UserMapper;
import com.trace.security.JwtUtil;
import com.trace.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (userMapper.existsByUsername(request.getUsername()))
            throw new IllegalArgumentException("用户名已存在");
        if (userMapper.existsByEmail(request.getEmail()))
            throw new IllegalArgumentException("邮箱已被注册");
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail()).build();
        userMapper.insert(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("userId", user.getId(), "username", user.getUsername(),
                "email", user.getEmail(), "token", token);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        // 优先通过邮箱查找，其次用户名
        User user = null;
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            user = userMapper.findByEmail(request.getEmail());
        } else if (request.getUsername() != null && !request.getUsername().isBlank()) {
            user = userMapper.findByUsername(request.getUsername());
        }
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("用户名或密码错误");
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("userId", user.getId(), "username", user.getUsername(),
                "email", user.getEmail(), "token", token);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userMapper.findByEmail(email);
        if (user == null) throw new IllegalArgumentException("该邮箱未注册");
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public String updateAvatar(Long userId, MultipartFile file) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        try {
            String base64 = "data:" + file.getContentType() + ";base64," +
                    Base64.getEncoder().encodeToString(file.getBytes());
            u.setAvatarUrl(base64);
            userMapper.updateById(u);
            return u.getAvatarUrl();
        } catch (IOException e) {
            throw new RuntimeException("头像上传失败", e);
        }
    }

    @Override
    @Transactional
    public String updateUsername(Long userId, String newUsername) {
        if (newUsername == null || newUsername.trim().length() < 3)
            throw new IllegalArgumentException("用户名至少3个字符");
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        if (userMapper.existsByUsername(newUsername) && !u.getUsername().equals(newUsername))
            throw new IllegalArgumentException("用户名已被占用，请换一个");
        u.setUsername(newUsername.trim());
        userMapper.updateById(u);
        return u.getUsername();
    }

    @Override
    public User getProfile(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new IllegalArgumentException("用户不存在");
        return u;
    }
}
