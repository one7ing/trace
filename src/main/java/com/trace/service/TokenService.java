package com.trace.service;

public interface TokenService {

    /** 将refreshToken存入Redis，Key=trace:refresh:{userId}，TTL=7天 */
    void storeRefreshToken(Long userId, String refreshToken);

    /** 校验refreshToken：解析+Redis比对，通过返回userId，否则返回null */
    Long validateAndGetUserId(String refreshToken);

    /** 删除Redis中的refreshToken，用于登出 */
    void deleteRefreshToken(Long userId);

    /** 用refreshToken换取新的accessToken，校验通过则生成新token返回，失败返回null */
    String refreshAccessToken(String refreshToken);
}
