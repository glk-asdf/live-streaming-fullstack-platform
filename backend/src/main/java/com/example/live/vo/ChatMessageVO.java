package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "弹幕消息")
public record ChatMessageVO(
        @Schema(description = "消息 ID") Long id,
        @Schema(description = "房间 ID") Long roomId,
        @Schema(description = "发送者 ID") Long userId,
        @Schema(description = "发送者昵称") String nickname,
        @Schema(description = "发送者头像") String avatar,
        @Schema(description = "内容") String content,
        @Schema(description = "类型：0 文本 / 2 系统") Integer type,
        @Schema(description = "发送时间") LocalDateTime createdAt) {
}
