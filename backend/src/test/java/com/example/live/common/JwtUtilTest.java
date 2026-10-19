package com.example.live.common;

import com.example.live.common.util.JwtUtil;
import com.example.live.config.JwtProperties;
import com.example.live.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Test
    void generatedTokenCarriesIdentityClaims() {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(SECRET, 3600, 7200));
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setRole("USER");

        String token = jwtUtil.generateAccessToken(user);
        Claims claims = jwtUtil.parse(token);

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void expiredTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil(new JwtProperties(SECRET, -60, 7200));
        User user = new User();
        user.setId(7L);
        user.setUsername("alice");
        user.setRole("USER");

        String token = jwtUtil.generateAccessToken(user);

        assertThatThrownBy(() -> jwtUtil.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtUtil(new JwtProperties("too-short", 3600, 7200)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
