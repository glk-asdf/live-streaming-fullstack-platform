package com.example.live.service.impl;

import com.example.live.common.constant.RoomStatus;
import com.example.live.entity.LiveRoom;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.ws.RoomEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 房间状态流转的唯一入口：鉴权回调、媒体事件、定时对账都走这里。
 */
@Service
public class RoomStateService {

    private static final Logger log = LoggerFactory.getLogger(RoomStateService.class);

    private final LiveRoomMapper liveRoomMapper;
    private final RoomEventPublisher roomEventPublisher;

    public RoomStateService(LiveRoomMapper liveRoomMapper, RoomEventPublisher roomEventPublisher) {
        this.liveRoomMapper = liveRoomMapper;
        this.roomEventPublisher = roomEventPublisher;
    }

    @Transactional
    public void markLive(Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null || room.getStatus() != RoomStatus.IDLE) {
            return;
        }
        room.setStatus(RoomStatus.LIVE);
        if (room.getLiveStartTime() == null) {
            room.setLiveStartTime(LocalDateTime.now());
        }
        room.setLiveEndTime(null);
        liveRoomMapper.updateById(room);
        log.info("room {} -> LIVE", roomId);
    }

    @Transactional
    public void markEnded(Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null || room.getStatus() != RoomStatus.LIVE) {
            return;
        }
        room.setStatus(RoomStatus.ENDED);
        room.setLiveEndTime(LocalDateTime.now());
        room.setPeakViewer(Math.max(
                room.getPeakViewer() == null ? 0 : room.getPeakViewer(),
                room.getViewerCount() == null ? 0 : room.getViewerCount()));
        liveRoomMapper.updateById(room);
        roomEventPublisher.roomEnded(roomId);
        log.info("room {} -> ENDED", roomId);
    }
}
