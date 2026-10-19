package com.example.live.common.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * MediaMTX path 与推流 query 的解析工具。
 */
public final class MediaPathUtil {

    public static final String ROOM_PATH_PREFIX = "live/";

    private MediaPathUtil() {
    }

    /**
     * 从 {@code live/{roomId}} 中取出房间 ID；格式不合法返回 null。
     */
    public static Long roomId(String path) {
        if (path == null || !path.startsWith(ROOM_PATH_PREFIX)) {
            return null;
        }
        String idPart = path.substring(ROOM_PATH_PREFIX.length());
        if (idPart.isEmpty() || idPart.contains("/")) {
            return null;
        }
        try {
            return Long.valueOf(idPart);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 从 RTMP query（如 {@code key=abc}）中取出推流密钥。
     */
    public static String streamKey(String query) {
        return queryParam(query, "key");
    }

    public static String queryParam(String query, String name) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int index = pair.indexOf('=');
            if (index <= 0) {
                continue;
            }
            if (name.equals(pair.substring(0, index))) {
                return URLDecoder.decode(pair.substring(index + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    public static String roomPath(Long roomId) {
        return ROOM_PATH_PREFIX + roomId;
    }
}
