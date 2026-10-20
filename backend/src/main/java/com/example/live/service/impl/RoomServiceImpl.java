package com.example.live.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.live.common.constant.RoomStatus;
import com.example.live.common.exception.BizException;
import com.example.live.common.result.PageResult;
import com.example.live.common.result.ResultCode;
import com.example.live.common.util.MediaPathUtil;
import com.example.live.common.util.SecurityUtil;
import com.example.live.config.MediaProperties;
import com.example.live.converter.LiveRoomConverter;
import com.example.live.dto.RoomSaveRequest;
import com.example.live.entity.LiveRoom;
import com.example.live.entity.User;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.mapper.UserMapper;
import com.example.live.service.RoomService;
import com.example.live.vo.LiveRoomVO;
import com.example.live.vo.RoomStreamVO;
import com.example.live.ws.RoomEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;
    private final RoomStateService roomStateService;
    private final MediaApiClient mediaApiClient;
    private final MediaProperties mediaProperties;
    private final RoomEventPublisher roomEventPublisher;

    public RoomServiceImpl(LiveRoomMapper liveRoomMapper, UserMapper userMapper,
                           RoomStateService roomStateService, MediaApiClient mediaApiClient,
                           MediaProperties mediaProperties, RoomEventPublisher roomEventPublisher) {
        this.liveRoomMapper = liveRoomMapper;
        this.userMapper = userMapper;
        this.roomStateService = roomStateService;
        this.mediaApiClient = mediaApiClient;
        this.mediaProperties = mediaProperties;
        this.roomEventPublisher = roomEventPublisher;
    }

    @Override
    public PageResult<LiveRoomVO> pageRooms(int page, int size, Long categoryId, String keyword, Integer status) {
        int filterStatus = status == null ? RoomStatus.LIVE : status;
        Page<LiveRoom> pageParam = new Page<>(page, size);
        Page<LiveRoom> result = liveRoomMapper.selectPage(pageParam,
                Wrappers.<LiveRoom>lambdaQuery()
                        .eq(LiveRoom::getStatus, filterStatus)
                        .eq(categoryId != null, LiveRoom::getCategoryId, categoryId)
                        .like(StringUtils.hasText(keyword), LiveRoom::getTitle, keyword)
                        .orderByDesc(LiveRoom::getHotScore)
                        .orderByDesc(LiveRoom::getCreatedAt)
                        .orderByDesc(LiveRoom::getId));
        Map<Long, User> anchors = loadAnchors(result.getRecords());
        return PageResult.of(result, room ->
                LiveRoomConverter.toVO(room, anchors.get(room.getAnchorId())));
    }

    @Override
    public LiveRoomVO myRoom() {
        Long userId = SecurityUtil.currentUserId();
        LiveRoom room = liveRoomMapper.selectOne(
                Wrappers.<LiveRoom>lambdaQuery()
                        .eq(LiveRoom::getAnchorId, userId)
                        .orderByDesc(LiveRoom::getCreatedAt)
                        .orderByDesc(LiveRoom::getId)
                        .last("LIMIT 1"));
        return room == null ? null : LiveRoomConverter.toVO(room, userMapper.selectById(userId));
    }

    @Override
    public LiveRoomVO detail(Long id) {
        LiveRoom room = getRoom(id);
        return LiveRoomConverter.toVO(room, userMapper.selectById(room.getAnchorId()));
    }

    @Override
    @Transactional
    public LiveRoomVO create(RoomSaveRequest request) {
        Long userId = SecurityUtil.currentUserId();
        Long activeCount = liveRoomMapper.selectCount(
                Wrappers.<LiveRoom>lambdaQuery()
                        .eq(LiveRoom::getAnchorId, userId)
                        .in(LiveRoom::getStatus, RoomStatus.IDLE, RoomStatus.LIVE));
        if (activeCount != null && activeCount > 0) {
            throw new BizException(ResultCode.CONFLICT, "你已有未结束的直播间");
        }

        LiveRoom room = new LiveRoom();
        room.setAnchorId(userId);
        room.setTitle(request.title().trim());
        room.setCover(normalize(request.cover()));
        room.setCategoryId(request.categoryId());
        room.setStreamKey(generateStreamKey());
        room.setStatus(RoomStatus.IDLE);
        room.setViewerCount(0);
        room.setPeakViewer(0);
        room.setLikeCount(0L);
        room.setHotScore(0);
        room.setReplayStatus(0);
        liveRoomMapper.insert(room);
        promoteToAnchor(userId);
        return LiveRoomConverter.toVO(room);
    }

    @Override
    @Transactional
    public LiveRoomVO update(Long id, RoomSaveRequest request) {
        LiveRoom room = getOwnedRoom(id);
        room.setTitle(request.title().trim());
        room.setCover(normalize(request.cover()));
        room.setCategoryId(request.categoryId());
        liveRoomMapper.updateById(room);
        return LiveRoomConverter.toVO(room);
    }

    @Override
    @Transactional
    public RoomStreamVO start(Long id) {
        LiveRoom room = getOwnedRoom(id);
        if (Integer.valueOf(RoomStatus.LIVE).equals(room.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "直播间正在直播中");
        }
        if (Integer.valueOf(RoomStatus.BANNED).equals(room.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "直播间已被封禁");
        }

        room.setStreamKey(generateStreamKey());
        room.setStatus(RoomStatus.IDLE);
        room.setLiveStartTime(null);
        room.setLiveEndTime(null);
        room.setViewerCount(0);
        room.setPeakViewer(0);
        room.setHotScore(0);
        liveRoomMapper.updateById(room);
        return toStream(room);
    }

    @Override
    @Transactional
    public RoomStreamVO resetStreamKey(Long id) {
        LiveRoom room = getOwnedRoom(id);
        if (Integer.valueOf(RoomStatus.LIVE).equals(room.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "直播中不能重置密钥，请先结束直播");
        }
        room.setStreamKey(generateStreamKey());
        liveRoomMapper.updateById(room);
        return toStream(room);
    }

    @Override
    public RoomStreamVO streamInfo(Long id) {
        return toStream(getOwnedRoom(id));
    }

    @Override
    @Transactional
    public void stop(Long id) {
        LiveRoom room = getOwnedRoom(id);
        if (Integer.valueOf(RoomStatus.ENDED).equals(room.getStatus())) {
            return;
        }

        room.setStatus(RoomStatus.ENDED);
        room.setLiveEndTime(LocalDateTime.now());
        room.setPeakViewer(Math.max(
                room.getPeakViewer() == null ? 0 : room.getPeakViewer(),
                room.getViewerCount() == null ? 0 : room.getViewerCount()));
        liveRoomMapper.updateById(room);
        roomEventPublisher.roomEnded(id);

        try {
            mediaApiClient.kickPublisher(MediaPathUtil.roomPath(id));
        } catch (Exception ex) {
            // 踢流失败不影响关播，定时对账会兜底
            log.warn("kick publisher failed, roomId={}", id, ex);
        }
    }

    @Override
    public boolean authenticatePublish(Long roomId, String streamKey) {
        if (!StringUtils.hasText(streamKey)) {
            return false;
        }
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null || room.getStatus() == null) {
            return false;
        }
        if (room.getStatus() == RoomStatus.BANNED || room.getStatus() == RoomStatus.ENDED) {
            return false;
        }
        if (!streamKey.equals(room.getStreamKey())) {
            return false;
        }
        User anchor = userMapper.selectById(room.getAnchorId());
        if (anchor == null || Integer.valueOf(2).equals(anchor.getStatus())) {
            return false;
        }
        roomStateService.markLive(roomId);
        return true;
    }

    private Map<Long, User> loadAnchors(List<LiveRoom> rooms) {
        Set<Long> anchorIds = rooms.stream()
                .map(LiveRoom::getAnchorId)
                .collect(Collectors.toSet());
        if (anchorIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectByIds(anchorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private LiveRoom getRoom(Long id) {
        LiveRoom room = liveRoomMapper.selectById(id);
        if (room == null) {
            throw new BizException(ResultCode.NOT_FOUND, "直播间不存在");
        }
        return room;
    }

    private LiveRoom getOwnedRoom(Long id) {
        LiveRoom room = getRoom(id);
        if (!room.getAnchorId().equals(SecurityUtil.currentUserId())) {
            throw new BizException(ResultCode.FORBIDDEN, "只能操作自己的直播间");
        }
        if (Integer.valueOf(RoomStatus.BANNED).equals(room.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "直播间已被封禁");
        }
        return room;
    }

    private RoomStreamVO toStream(LiveRoom room) {
        String server = "rtmp://" + mediaProperties.rtmpHost() + ":" + mediaProperties.rtmpPort() + "/live";
        String streamKey = room.getId() + "?key=" + room.getStreamKey();
        return new RoomStreamVO(room.getId(), server, streamKey, server + "/" + streamKey, room.getStatus());
    }

    private void promoteToAnchor(Long userId) {
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .eq(User::getRole, "USER")
                .set(User::getRole, "ANCHOR"));
    }

    private String generateStreamKey() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
