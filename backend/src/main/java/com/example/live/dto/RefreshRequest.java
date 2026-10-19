package com.example.live.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "刷新令牌请求")
public record RefreshRequest(
        @NotBlank(message = "refreshToken 不能为空") String refreshToken) {
}
