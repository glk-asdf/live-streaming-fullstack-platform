package com.example.live.security;

/**
 * 放进 SecurityContext 的登录用户信息，不包含密码等敏感字段。
 */
public record LoginUser(Long id, String username, String role) {
}
