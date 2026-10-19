package com.example.live.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登出请求；refreshToken 可为空，为空时只结束本次前端会话")
public record LogoutRequest(String refreshToken) {
}
