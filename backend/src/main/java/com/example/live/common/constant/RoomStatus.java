package com.example.live.common.constant;

/**
 * 直播间状态。与 t_live_room.status 注释保持一致。
 */
public final class RoomStatus {

    public static final int IDLE = 0;
    public static final int LIVE = 1;
    public static final int ENDED = 2;
    public static final int BANNED = 3;

    private RoomStatus() {
    }
}
