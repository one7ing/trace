package com.trace.service.impl;

import com.constant.constant;
import com.trace.security.JwtUtil;
import com.trace.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;

    /** 将refreshToken存入Redis，Key=trace:refresh:{userId}，TTL=7天 */
    @Override
    public void storeRefreshToken(Long userId, String refreshToken) {
        String key = constant.Token.REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken,
                Duration.ofMillis(constant.Token.REFRESH_EXPIRATION_MS));
    }

    /** 校验refreshToken：解析Token获取userId，再从Redis比对是否匹配 */
    @Override
    public Long validateAndGetUserId(String refreshToken) {
        try {
            // 解析Token获取userId
            Long userId = jwtUtil.getUserId(refreshToken);
            // 从Redis查询存储的refreshToken
            String key = constant.Token.REFRESH_TOKEN_PREFIX + userId;
            String storedToken = (String) redisTemplate.opsForValue().get(key);
            // 比对是否匹配
            if (storedToken != null && storedToken.equals(refreshToken)) {
                return userId;
            }
        } catch (Exception e) {
            log.debug("refreshToken校验失败: {}", e.getMessage());
        }
        return null;
    }

    /** 删除Redis中的refreshToken，用于登出 */
    @Override
    public void deleteRefreshToken(Long userId) {
        String key = constant.Token.REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.delete(key);
    }

    /** 用refreshToken换取新的accessToken */
    @Override
    public String refreshAccessToken(String refreshToken) {
        // 先校验refreshToken是否有效
        Long userId = validateAndGetUserId(refreshToken);
        if (userId == null) {
            return null;
        }
        // 解析出username，生成新的accessToken
        String username = jwtUtil.getUsername(refreshToken);
        return jwtUtil.generateAccessToken(userId, username);
    }
}
