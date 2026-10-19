package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录/刷新结果")
public record AuthResponse(
        @Schema(description = "accessToken，放入 Authorization: Bearer") String accessToken,
        @Schema(description = "refreshToken，仅用于换新令牌") String refreshToken,
        @Schema(description = "accessToken 有效期（秒）") long expiresIn,
        @Schema(description = "当前用户资料") UserVO user) {
}
