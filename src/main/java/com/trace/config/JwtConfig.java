package com.trace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** JWT签名密钥 */
    private String secret;
    /** 短token（accessToken）过期时间，单位毫秒，默认30分钟 */
    private long accessExpiration = 30 * 60 * 1000L;
    /** 长token（refreshToken）过期时间，单位毫秒，默认7天 */
    private long refreshExpiration = 7 * 24 * 60 * 60 * 1000L;
}
