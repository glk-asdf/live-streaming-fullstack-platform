package com.example.live.service;

import com.example.live.common.constant.RoomStatus;
import com.example.live.config.MediaProperties;
import com.example.live.entity.LiveRoom;
import com.example.live.entity.User;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.mapper.UserMapper;
import com.example.live.service.impl.MediaApiClient;
import com.example.live.service.impl.RoomServiceImpl;
import com.example.live.service.impl.RoomStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    @Mock
    private LiveRoomMapper liveRoomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoomStateService roomStateService;
    @Mock
    private MediaApiClient mediaApiClient;

    private RoomServiceImpl roomService;

    @BeforeEach
    void setUp() {
        MediaProperties properties = new MediaProperties(
                "localhost", 1935, "http://localhost:8888", "http://localhost:8889",
                "http://127.0.0.1:9997", "test-callback-token");
        roomService = new RoomServiceImpl(
                liveRoomMapper, userMapper, roomStateService, mediaApiClient, properties);
    }

    @Test
    void validKeyAndActiveAnchorCanPublish() {
        LiveRoom room = room(7L, 1L, RoomStatus.IDLE);
        User anchor = user(1L, 0);
        when(liveRoomMapper.selectById(7L)).thenReturn(room);
        when(userMapper.selectById(1L)).thenReturn(anchor);

        assertThat(roomService.authenticatePublish(7L, "stream-key")).isTrue();
        verify(roomStateService).markLive(7L);
    }

    @Test
    void bannedAnchorCannotPublish() {
        LiveRoom room = room(7L, 1L, RoomStatus.IDLE);
        User anchor = user(1L, 2);
        when(liveRoomMapper.selectById(7L)).thenReturn(room);
        when(userMapper.selectById(1L)).thenReturn(anchor);

        assertThat(roomService.authenticatePublish(7L, "stream-key")).isFalse();
        verify(roomStateService, never()).markLive(anyLong());
    }

    @Test
    void bannedRoomCannotPublishEvenWithValidKey() {
        LiveRoom room = room(7L, 1L, RoomStatus.BANNED);
        when(liveRoomMapper.selectById(7L)).thenReturn(room);

        assertThat(roomService.authenticatePublish(7L, "stream-key")).isFalse();
        verify(roomStateService, never()).markLive(anyLong());
    }

    private LiveRoom room(Long id, Long anchorId, int status) {
        LiveRoom room = new LiveRoom();
        room.setId(id);
        room.setAnchorId(anchorId);
        room.setStatus(status);
        room.setStreamKey("stream-key");
        return room;
    }

    private User user(Long id, int status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        return user;
    }
}
