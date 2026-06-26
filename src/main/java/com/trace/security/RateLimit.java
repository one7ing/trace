package com.trace.security;

import java.lang.annotation.*;

/**
 * 滑动窗口限流注解。
 * 标记在 Controller 方法或类上，通过 Redis ZSET + Lua 脚本实现原子限流。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** Redis key 前缀，如 "rate:chat"，最终 key = rate:{key}:{userId} */
    String key();

    /** 窗口内最大请求数，默认 10 */
    int limit() default 10;

    /** 滑动窗口秒数，默认 60 */
    int windowSeconds() default 60;
}
