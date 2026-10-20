package com.example.live.ws;

/**
 * WebSocket 统一下行信封。
 */
public record WsMessage<T>(String type, T data, long ts) {

    public static <T> WsMessage<T> of(String type, T data) {
        return new WsMessage<>(type, data, System.currentTimeMillis());
    }
}
