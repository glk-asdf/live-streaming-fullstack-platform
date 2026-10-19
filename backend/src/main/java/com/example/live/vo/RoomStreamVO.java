package com.example.live.vo;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OBS 推流信息")
public record RoomStreamVO(
        @Schema(description = "房间 ID") Long roomId,
        @Schema(description = "OBS 服务器地址") String server,
        @Schema(description = "OBS 串流密钥（含 key 参数）") String streamKey,
        @Schema(description = "完整 RTMP 地址") String url,
        @Schema(description = "房间状态") Integer status) {
}
