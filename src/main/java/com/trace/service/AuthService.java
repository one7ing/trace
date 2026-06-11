package com.trace.service;

import com.trace.dto.LoginRequest;
import com.trace.dto.RegisterRequest;

import java.util.Map;

public interface AuthService {

    Map<String, Object> register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);
}
