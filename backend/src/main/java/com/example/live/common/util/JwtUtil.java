package com.example.live.common.util;

import com.example.live.config.JwtProperties;
import com.example.live.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * accessToken 的签发与解析。refreshToken 是不透明随机串，存 Redis，不走 JWT。
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessExpireSeconds;

    public JwtUtil(JwtProperties properties) {
        byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("live.jwt.secret 至少需要 32 字节");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.accessExpireSeconds = properties.accessExpireSeconds();
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessExpireSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getAccessExpireSeconds() {
        return accessExpireSeconds;
    }
}
