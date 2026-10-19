package com.example.live.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 与刷新令牌配置。
 *
 * @param secret               HMAC 密钥，至少 32 字节
 * @param accessExpireSeconds  accessToken 有效期（秒）
 * @param refreshExpireSeconds refreshToken 有效期（秒）
 */
@ConfigurationProperties(prefix = "live.jwt")
public record JwtProperties(
        String secret,
        long accessExpireSeconds,
        long refreshExpireSeconds) {
}
