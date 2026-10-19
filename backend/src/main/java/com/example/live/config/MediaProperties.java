package com.example.live.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 媒体服务器地址配置，P3 起用于下发推流与播放地址。
 *
 * @param rtmpHost   RTMP 主机名（对外可访问）
 * @param rtmpPort   RTMP 端口
 * @param hlsBase    LL-HLS 基础地址
 * @param webrtcBase WHEP 基础地址
 * @param apiBase    MediaMTX 管理 API 地址（仅后端内网访问）
 */
@ConfigurationProperties(prefix = "live.media")
public record MediaProperties(
        String rtmpHost,
        int rtmpPort,
        String hlsBase,
        String webrtcBase,
        String apiBase) {
}
