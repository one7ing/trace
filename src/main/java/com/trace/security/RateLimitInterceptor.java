package com.trace.security;

import com.constant.constant;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * 滑动窗口限流拦截器。
 * 在 preHandle 中读取方法上的 @RateLimit 注解，通过预加载的 Lua 脚本对 Redis ZSET 做原子限流。
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 预加载的 Lua 脚本 */
    private static final RedisScript<Long>  RATE_LIMIT_SCRIPT;

    static {
        // Lua: 滑动窗口限流，基于 ZSET
        // KEYS[1] = rate key, ARGV[1] = window (秒), ARGV[2] = limit, ARGV[3] = now (毫秒)
        RATE_LIMIT_SCRIPT = RedisScript.of(new ClassPathResource("scripts/xl.lua"), Long.class);
    }

    public RateLimitInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(@NotNull HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) throws Exception {
        // 只处理原始请求，跳过 ASYNC/ERROR/FORWARD/INCLUDE 等二次分发
        // （否则 Flux/SSE 流式返回会在 ASYNC 阶段再次触发限流计数）
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            return true;
        }

        // 只对 Controller 方法生效
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }

        // 读取方法或类上的 @RateLimit 注解
        RateLimit limit = hm.getMethodAnnotation(RateLimit.class);
        if (limit == null) {
            limit = hm.getBeanType().getAnnotation(RateLimit.class);
        }
        if (limit == null) {
            return true;
        }

        // 获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            // 未认证用户不限流（由 SecurityConfig 拦截认证）
            return true;
        }

        String key = constant.RateLimit.KEY_PREFIX + limit.key() + ":" + userId;

        long now = System.currentTimeMillis();
        Long result = stringRedisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                List.of(key),
                String.valueOf(limit.windowSeconds()),
                String.valueOf(limit.limit()),
                String.valueOf(now)
        );

        if (result == 1L) {
            return true;
        }

        // 限流触发
        log.warn("Rate limit triggered: key={}, userId={}, limit={}/{}s",
                limit.key(), userId, limit.limit(), limit.windowSeconds());

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("code", constant.RateLimit.CODE);
        body.put("message", "请求过于频繁，请稍后再试");
        body.put("data", null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return false;
    }
}
