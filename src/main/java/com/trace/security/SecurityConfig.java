package com.trace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 自定义的 JWT 认证过滤器，用来从请求中提取 Token 并验证身份 */
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 静态初始化块。
     * 设置 SecurityContext 的线程继承模式，确保在异步线程（如 @Async、虚拟线程）中也能获取当前用户信息。
     */
    static {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    /**
     * 定义安全过滤链（Security Filter Chain）。
     * 这是 Spring Security 的核心配置，所有请求都会经过这条链上的过滤器。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 配置跨域（CORS）：允许前端开发服务器访问后端 API
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 2. 禁用 CSRF 保护：前后端分离 + 无状态 Token，不需要 CSRF
                .csrf(AbstractHttpConfigurer::disable)

                // 3. 会话管理：设为无状态（STATELESS），服务器不保存 Session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 4. 接口权限配置
                .authorizeHttpRequests(auth -> auth
                        // 登录、注册等认证接口：直接放行，不需要 Token
                        .requestMatchers("/api/auth/**").permitAll()
                        // Swagger / Knife4j 接口文档页面：直接放行
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/webjars/**",
                                "/swagger-resources/**", "/swagger-ui/**").permitAll()
                        // 其他所有 /api/** 接口：必须携带有效 Token 才能访问
                        .requestMatchers("/api/**").authenticated()
                        // 其他请求（如静态资源）：直接放行
                        .anyRequest().permitAll()
                )

                // 5. 在 Spring Security 的认证过滤器之前，插入我们自定义的 JWT 过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 6. 自定义未认证返回JSON（替代默认的重定向/403页面）
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            // 使用HashMap替代Map.of，因为Map.of不接受null值
                            java.util.Map<String, Object> body = new java.util.HashMap<>();
                            body.put("code", 40100);
                            body.put("message", "未认证，请先登录");
                            body.put("data", null);
                            response.getWriter().write(objectMapper.writeValueAsString(body));
                        })
                )

                // 7. 显式保存安全上下文
                .securityContext(securityContext -> securityContext.requireExplicitSave(false));

        return http.build();
    }

    /**
     * 密码编码器。
     * 使用 BCrypt 加密密码，注册和登录时会用到。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 跨域（CORS）配置源。
     * 允许指定前端地址访问后端 API，支持跨域携带 Cookie 和自定义头。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许访问后端的前端地址（开发环境）
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://localhost:3000"));
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // 允许所有请求头
        configuration.setAllowedHeaders(List.of("*"));
        // 允许携带 Cookie
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径生效
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
