package com.example.live.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * MediaMTX HTTP 鉴权回调请求体。仅使用本业务需要的字段。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MediaAuthRequest(
        String user,
        String password,
        String token,
        String ip,
        String action,
        String path,
        String protocol,
        String id,
        String query) {
}
