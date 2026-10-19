package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "直播间列表项")
public record LiveRoomVO(
        @Schema(description = "房间 ID") Long id,
        @Schema(description = "主播 ID") Long anchorId,
        @Schema(description = "分类 ID") Long categoryId,
        @Schema(description = "标题") String title,
        @Schema(description = "封面地址") String cover,
        @Schema(description = "状态：0 未开播 / 1 直播中 / 2 已结束 / 3 封禁") Integer status,
        @Schema(description = "实时在线人数") Integer viewerCount,
        @Schema(description = "本场峰值在线") Integer peakViewer,
        @Schema(description = "点赞数") Long likeCount,
        @Schema(description = "热度分") Integer hotScore,
        @Schema(description = "开播时间") LocalDateTime liveStartTime,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
