package com.example.live.converter;

import com.example.live.entity.LiveRoom;
import com.example.live.vo.LiveRoomVO;

public final class LiveRoomConverter {

    private LiveRoomConverter() {
    }

    public static LiveRoomVO toVO(LiveRoom room) {
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
                room.getCreatedAt()
        );
    }
}
