package com.example.live.service.impl;

import com.example.live.ws.ViewerCountData;
import com.example.live.ws.WsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线人数按 WebSocket 连接数统计，Redis ZSET 存 sessionId，score 为最后心跳时间。
 * 单实例的 sessionId -> rooms 映射用于断连清理；P12 多实例时改为广播/集中存储。
 */
@Service
public class ViewerService {

    private static final Logger log = LoggerFactory.getLogger(ViewerService.class);
    private static final long STALE_MILLIS = 30_000;
    private static final Duration VIEWER_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final Set<Long> activeRooms = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<String, Set<Long>> sessionRooms = new ConcurrentHashMap<>();

    public ViewerService(StringRedisTemplate redisTemplate, SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    public void join(String sessionId, Long roomId) {
        sessionRooms.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(roomId);
        activeRooms.add(roomId);
        touch(roomId, sessionId);
        broadcastCount(roomId);
    }

    public void heartbeat(Long roomId, String sessionId) {
        if (sessionId == null || roomId == null || !activeRooms.contains(roomId)) {
            return;
        }
        touch(roomId, sessionId);
    }

    public void disconnect(String sessionId) {
        Set<Long> rooms = sessionRooms.remove(sessionId);
        if (rooms == null) {
            return;
        }
        for (Long roomId : rooms) {
            leave(roomId, sessionId);
        }
    }

    public void leave(Long roomId, String sessionId) {
        try {
            redisTemplate.opsForZSet().remove(key(roomId), sessionId);
            long count = count(roomId);
            if (count <= 0) {
                activeRooms.remove(roomId);
                redisTemplate.delete(key(roomId));
            }
            broadcastCount(roomId, count);
        } catch (Exception ex) {
            log.warn("remove viewer failed, roomId={}", roomId, ex);
        }
    }

    public void clear(Long roomId) {
        try {
            redisTemplate.delete(key(roomId));
            activeRooms.remove(roomId);
            broadcastCount(roomId, 0);
        } catch (Exception ex) {
            log.warn("clear viewers failed, roomId={}", roomId, ex);
        }
    }

    public long count(Long roomId) {
        Long count = redisTemplate.opsForZSet().zCard(key(roomId));
        return count == null ? 0 : count;
    }

    @Scheduled(fixedDelayString = "${live.viewer.cleanup-interval-ms:10000}")
    public void cleanupExpired() {
        double cutoff = nowSeconds() - STALE_MILLIS / 1000.0;
        for (Long roomId : Set.copyOf(activeRooms)) {
            try {
                Long removed = redisTemplate.opsForZSet()
                        .removeRangeByScore(key(roomId), 0, cutoff);
                long count = count(roomId);
                if (removed != null && removed > 0) {
                    broadcastCount(roomId, count);
                }
                if (count <= 0) {
                    activeRooms.remove(roomId);
                    redisTemplate.delete(key(roomId));
                }
            } catch (Exception ex) {
                log.warn("cleanup viewers failed, roomId={}", roomId, ex);
            }
        }
    }

    private void touch(Long roomId, String sessionId) {
        try {
            String key = key(roomId);
            redisTemplate.opsForZSet().add(key, sessionId, nowSeconds());
            redisTemplate.expire(key, VIEWER_TTL);
        } catch (Exception ex) {
            log.warn("touch viewer failed, roomId={}", roomId, ex);
        }
    }

    private void broadcastCount(Long roomId) {
        try {
            broadcastCount(roomId, count(roomId));
        } catch (Exception ex) {
            log.warn("count viewers failed, roomId={}", roomId, ex);
        }
    }

    private void broadcastCount(Long roomId, long count) {
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId,
                    WsMessage.of("VIEWER_COUNT", new ViewerCountData(roomId, count)));
        } catch (Exception ex) {
            log.warn("broadcast viewer count failed, roomId={}", roomId, ex);
        }
    }

    private double nowSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    private String key(Long roomId) {
        return "live:viewers:" + roomId;
    }
}
