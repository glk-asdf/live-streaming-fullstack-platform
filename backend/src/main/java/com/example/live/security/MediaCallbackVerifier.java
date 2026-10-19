package com.example.live.security;

import com.example.live.config.MediaProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 校验 MediaMTX 回调来源：共享 token 优先，本机回环地址兜底（仅本地开发）。
 */
@Component
public class MediaCallbackVerifier {

    private final MediaProperties mediaProperties;

    public MediaCallbackVerifier(MediaProperties mediaProperties) {
        this.mediaProperties = mediaProperties;
    }

    public boolean isTrusted(HttpServletRequest request) {
        String token = request.getParameter("token");
        if (StringUtils.hasText(token)) {
            return constantTimeEquals(token, mediaProperties.callbackToken());
        }
        String remote = request.getRemoteAddr();
        return "127.0.0.1".equals(remote)
                || "0:0:0:0:0:0:0:1".equals(remote)
                || "::1".equals(remote);
    }

    private boolean constantTimeEquals(String actual, String expected) {
        if (expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }
}
