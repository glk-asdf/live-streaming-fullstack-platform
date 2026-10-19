package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "播放地址与 5 分钟播放令牌")
public record PlayInfoVO(
        @Schema(description = "房间 ID") Long roomId,
        @Schema(description = "WHEP 播放地址（含 token）") String whepUrl,
        @Schema(description = "LL-HLS 播放地址（含 token）") String hlsUrl,
        @Schema(description = "播放令牌") String token,
        @Schema(description = "令牌过期时间") LocalDateTime expireAt) {
}
