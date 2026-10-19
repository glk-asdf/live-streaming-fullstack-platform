package com.example.live.service.impl;

import com.example.live.common.constant.RoomStatus;
import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.common.util.JwtUtil;
import com.example.live.common.util.SecurityUtil;
import com.example.live.config.MediaProperties;
import com.example.live.entity.LiveRoom;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.service.PlaybackService;
import com.example.live.vo.PlayInfoVO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class PlaybackServiceImpl implements PlaybackService {

    private final LiveRoomMapper liveRoomMapper;
    private final JwtUtil jwtUtil;
    private final MediaProperties mediaProperties;

    public PlaybackServiceImpl(LiveRoomMapper liveRoomMapper, JwtUtil jwtUtil,
                               MediaProperties mediaProperties) {
        this.liveRoomMapper = liveRoomMapper;
        this.jwtUtil = jwtUtil;
        this.mediaProperties = mediaProperties;
    }

    @Override
    public PlayInfoVO playInfo(Long roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ResultCode.NOT_FOUND, "直播间不存在");
        }
        if (!Integer.valueOf(RoomStatus.LIVE).equals(room.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "直播尚未开始或已经结束");
        }

        String token = jwtUtil.generatePlayToken(roomId, SecurityUtil.currentUserIdOrNull());
        String whep = baseUrl(mediaProperties.webrtcBase()) + "/live/" + roomId + "/whep?token=" + token;
        String hls = baseUrl(mediaProperties.hlsBase()) + "/live/" + roomId + "/index.m3u8?token=" + token;
        return new PlayInfoVO(
                roomId,
                whep,
                hls,
                token,
                LocalDateTime.now().plusSeconds(JwtUtil.PLAY_TOKEN_EXPIRE_SECONDS));
    }

    @Override
    public boolean canRead(Long roomId, String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = jwtUtil.parse(token);
            if (!Boolean.TRUE.equals(claims.get("play", Boolean.class))) {
                return false;
            }
            Number roomClaim = claims.get("roomId", Number.class);
            if (roomClaim == null || roomClaim.longValue() != roomId) {
                return false;
            }
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }

        LiveRoom room = liveRoomMapper.selectById(roomId);
        return room != null && Integer.valueOf(RoomStatus.LIVE).equals(room.getStatus());
    }

    private String baseUrl(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
