package com.example.live.controller;

import com.example.live.common.exception.BizException;
import com.example.live.common.result.Result;
import com.example.live.common.result.ResultCode;
import com.example.live.common.util.MediaPathUtil;
import com.example.live.dto.MediaAuthRequest;
import com.example.live.security.MediaCallbackVerifier;
import com.example.live.service.PlaybackService;
import com.example.live.service.RoomService;
import com.example.live.service.impl.RoomStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * MediaMTX 回调入口。调用方是媒体服务器，不经过 JWT，使用共享 token/回环地址校验。
 */
@Tag(name = "媒体回调")
@RestController
@RequestMapping("/api/v1/media")
public class MediaController {

    private static final Logger log = LoggerFactory.getLogger(MediaController.class);

    private final MediaCallbackVerifier callbackVerifier;
    private final RoomService roomService;
    private final PlaybackService playbackService;
    private final RoomStateService roomStateService;

    public MediaController(MediaCallbackVerifier callbackVerifier, RoomService roomService,
                           PlaybackService playbackService, RoomStateService roomStateService) {
        this.callbackVerifier = callbackVerifier;
        this.roomService = roomService;
        this.playbackService = playbackService;
        this.roomStateService = roomStateService;
    }

    @Operation(summary = "MediaMTX HTTP 鉴权回调")
    @PostMapping("/auth")
    public ResponseEntity<Void> auth(@RequestBody MediaAuthRequest request, HttpServletRequest httpRequest) {
        if (!callbackVerifier.isTrusted(httpRequest)) {
            log.warn("media auth rejected: untrusted callback source");
            return ResponseEntity.status(401).build();
        }
        Long roomId = MediaPathUtil.roomId(request.path());
        if (roomId == null) {
            log.info("media auth rejected: invalid path={}", request.path());
            return ResponseEntity.status(401).build();
        }

        boolean allowed = switch (request.action() == null ? "" : request.action()) {
            case "publish" -> roomService.authenticatePublish(
                    roomId, MediaPathUtil.streamKey(request.query()));
            case "read" -> playbackService.canRead(
                    roomId, MediaPathUtil.queryParam(request.query(), "token"));
            // 回放鉴权在 P8 接入，先放行由业务层控制可见性
            case "playback", "api", "metrics", "pprof" -> true;
            default -> false;
        };
        if (!allowed) {
            log.info("media auth denied: action={}, path={}", request.action(), request.path());
        }
        return allowed ? ResponseEntity.ok().build() : ResponseEntity.status(401).build();
    }

    @Operation(summary = "MediaMTX 推流上线事件")
    @PostMapping("/online")
    public Result<Void> online(@RequestParam String path, HttpServletRequest httpRequest) {
        ensureTrusted(httpRequest);
        Long roomId = requireRoomId(path);
        roomStateService.markLive(roomId);
        return Result.ok();
    }

    @Operation(summary = "MediaMTX 推流下线事件")
    @PostMapping("/offline")
    public Result<Void> offline(@RequestParam String path, HttpServletRequest httpRequest) {
        ensureTrusted(httpRequest);
        Long roomId = requireRoomId(path);
        roomStateService.markEnded(roomId);
        return Result.ok();
    }

    private void ensureTrusted(HttpServletRequest request) {
        if (!callbackVerifier.isTrusted(request)) {
            log.warn("media event rejected: untrusted callback source");
            throw new BizException(ResultCode.UNAUTHORIZED, "非法的媒体回调");
        }
    }

    private Long requireRoomId(String path) {
        Long roomId = MediaPathUtil.roomId(path);
        if (roomId == null) {
            throw new BizException(ResultCode.BAD_REQUEST, "非法的媒体 path");
        }
        return roomId;
    }
}
