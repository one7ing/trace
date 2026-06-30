package com.trace.security;

import com.constant.constant;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，负责 Token 的生成和解析 */
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 过滤器的核心逻辑。
     * 每个请求都会执行这个方法，用来检查和设置登录状态。
     */
    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain) throws ServletException, IOException {
        // 1. 从请求中提取 Token
        String token = extractToken(request);

        // 2. 如果 Token 存在，尝试解析并设置登录态
        if (StringUtils.hasText(token)) {
            try {
                // 解析Token，过期会抛出 ExpiredJwtException
                var claims = jwtUtil.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                String username = claims.get("username", String.class);

                // 3. 构建 Spring Security 的认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, username, Collections.emptyList());

                // 4. 将认证对象设置到当前线程的安全上下文中
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException e) {
                // Token过期，返回特定错误码供前端拦截器识别
                writeErrorResponse(response,
                        constant.Token.CODE_ACCESS_EXPIRED, "access token expired");
                return;
            } catch (Exception e) {
                // Token无效（签名错误、格式错误等）
                writeErrorResponse(response,
                        40100, "invalid token");
                return;
            }
        }

        // 5. 无论是否携带 Token，都继续执行后续的过滤链
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中提取 Token。
     * 支持两种方式：
     *   1. 从 Authorization 请求头（标准方式）：Bearer {token}
     *   2. 从 URL 参数 token（SSE EventSource 不支持自定义请求头时使用）
     */
    private String extractToken(HttpServletRequest request) {
        // 方式一：从 Authorization 请求头提取（大小写不敏感）
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.toLowerCase().startsWith("bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀
        }
        // 方式二：从 URL 参数提取（SSE 场景）
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }

    /** 向客户端写入JSON格式的错误响应 */
    private void writeErrorResponse(HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        // 使用HashMap替代Map.of，因为Map.of不接受null值
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
