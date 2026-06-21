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
        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail()).build();
        userMapper.insert(user);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("userId", user.getId(), "username", user.getUsername(), "token", token);
    }

    @Override
    public Map<String, Object> login(LoginRequest request) {
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash()))
            throw new BadCredentialsException("用户名或密码错误");
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return Map.of("userId", user.getId(), "username", user.getUsername(), "token", token);
    }
}
