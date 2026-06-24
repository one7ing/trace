package com.trace.security;

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

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** JWT 工具类，负责 Token 的生成和解析 */
    private final JwtUtil jwtUtil;

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

        // 2. 如果 Token 存在且合法，设置登录态
        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            // 从 Token 中解析出 userId 和 username
            Long userId = jwtUtil.getUserId(token);
            String username = jwtUtil.getUsername(token);

            // 3. 构建 Spring Security 的认证对象
            //    Collections.emptyList() 表示当前用户没有特殊角色权限（可根据需要扩展）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, username, Collections.emptyList());

            // 4. 将认证对象设置到当前线程的安全上下文中
            //    之后 Controller 中就可以通过 @AuthenticationPrincipal 拿到 userId
            SecurityContextHolder.getContext().setAuthentication(authentication);
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
        // 方式一：从 Authorization 请求头提取
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);  // 去掉 "Bearer " 前缀
        }
        // 方式二：从 URL 参数提取（SSE 场景）
        String queryToken = request.getParameter("token");
        if (StringUtils.hasText(queryToken)) {
            return queryToken;
        }
        return null;
    }
}