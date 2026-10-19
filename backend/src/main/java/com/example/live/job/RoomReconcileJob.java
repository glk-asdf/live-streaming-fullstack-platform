package com.example.live.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.live.common.constant.RoomStatus;
import com.example.live.entity.LiveRoom;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.service.impl.MediaApiClient;
import com.example.live.service.impl.RoomStateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 定时把 MediaMTX 的活跃 path 与数据库房间状态对齐。
 * 事件回调负责实时流转，本任务负责兜底（进程被杀、回调丢失）。
 */
@Component
public class RoomReconcileJob {

    private static final Logger log = LoggerFactory.getLogger(RoomReconcileJob.class);
    private static final long START_GRACE_SECONDS = 15;

    private final LiveRoomMapper liveRoomMapper;
    private final MediaApiClient mediaApiClient;
    private final RoomStateService roomStateService;

    public RoomReconcileJob(LiveRoomMapper liveRoomMapper, MediaApiClient mediaApiClient,
                            RoomStateService roomStateService) {
        this.liveRoomMapper = liveRoomMapper;
        this.mediaApiClient = mediaApiClient;
        this.roomStateService = roomStateService;
    }

    @Scheduled(fixedDelayString = "${live.room.reconcile-interval-ms:10000}")
    public void reconcile() {
        Set<String> activePaths;
        try {
            activePaths = mediaApiClient.activePaths();
        } catch (Exception ex) {
            log.warn("skip room reconcile, MediaMTX API unavailable: {}", ex.getMessage());
            return;
        }

        List<LiveRoom> rooms = liveRoomMapper.selectList(
                Wrappers.<LiveRoom>lambdaQuery()
                        .in(LiveRoom::getStatus, RoomStatus.IDLE, RoomStatus.LIVE));
        LocalDateTime now = LocalDateTime.now();
        for (LiveRoom room : rooms) {
            boolean active = activePaths.contains("live/" + room.getId());
            if (active && Integer.valueOf(RoomStatus.IDLE).equals(room.getStatus())) {
                roomStateService.markLive(room.getId());
            } else if (!active
                    && Integer.valueOf(RoomStatus.LIVE).equals(room.getStatus())
                    && room.getLiveStartTime() != null
                    && room.getLiveStartTime().isBefore(now.minusSeconds(START_GRACE_SECONDS))) {
                roomStateService.markEnded(room.getId());
            }
        }
    }
}
