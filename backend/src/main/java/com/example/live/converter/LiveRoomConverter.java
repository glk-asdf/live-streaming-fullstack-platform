package com.example.live.converter;

import com.example.live.entity.LiveRoom;
import com.example.live.entity.User;
import com.example.live.vo.LiveRoomVO;

public final class LiveRoomConverter {

    private LiveRoomConverter() {
    }

    public static LiveRoomVO toVO(LiveRoom room) {
        return toVO(room, null);
    }

    public static LiveRoomVO toVO(LiveRoom room, User anchor) {
        return new LiveRoomVO(
                room.getId(),
                room.getAnchorId(),
                room.getCategoryId(),
                room.getTitle(),
                room.getCover(),
                room.getStatus(),
                room.getViewerCount(),
                room.getPeakViewer(),
                room.getLikeCount(),
                room.getHotScore(),
                room.getLiveStartTime(),
                room.getLiveEndTime(),
                room.getCreatedAt(),
                anchor == null ? null : anchor.getNickname(),
                anchor == null ? null : anchor.getAvatar()
        );
    }
}
