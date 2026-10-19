package com.example.live.service;

import com.example.live.common.constant.RoomStatus;
import com.example.live.common.exception.BizException;
import com.example.live.common.util.JwtUtil;
import com.example.live.config.JwtProperties;
import com.example.live.config.MediaProperties;
import com.example.live.entity.LiveRoom;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.service.impl.PlaybackServiceImpl;
import com.example.live.vo.PlayInfoVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaybackServiceImplTest {

    private static final String SECRET = "0123456789012345678901234567890123456789";

    @Mock
    private LiveRoomMapper liveRoomMapper;

    private JwtUtil jwtUtil;
    private PlaybackServiceImpl playbackService;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(new JwtProperties(SECRET, 3600, 7200));
        MediaProperties mediaProperties = new MediaProperties(
                "localhost", 1935, "http://localhost:8888", "http://localhost:8889",
                "http://127.0.0.1:9997", "test-callback-token");
        playbackService = new PlaybackServiceImpl(liveRoomMapper, jwtUtil, mediaProperties);
    }

    @Test
    void liveRoomReturnsTokenizedHlsAndWhepUrls() {
        when(liveRoomMapper.selectById(9L)).thenReturn(room(9L, RoomStatus.LIVE));

        PlayInfoVO info = playbackService.playInfo(9L);

        assertThat(info.whepUrl())
                .startsWith("http://localhost:8889/live/9/whep?token=");
        assertThat(info.hlsUrl())
                .startsWith("http://localhost:8888/live/9/index.m3u8?token=");
        assertThat(info.token()).isNotBlank();
        assertThat(playbackService.canRead(9L, info.token())).isTrue();
    }

    @Test
    void tokenForAnotherRoomCannotRead() {
        String token = jwtUtil.generatePlayToken(8L, null);

        assertThat(playbackService.canRead(9L, token)).isFalse();
        assertThat(playbackService.canRead(9L, null)).isFalse();
    }

    @Test
    void nonLiveRoomCannotGetPlayInfo() {
        when(liveRoomMapper.selectById(9L)).thenReturn(room(9L, RoomStatus.IDLE));

        assertThatThrownBy(() -> playbackService.playInfo(9L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("直播");
    }

    private LiveRoom room(Long id, int status) {
        LiveRoom room = new LiveRoom();
        room.setId(id);
        room.setStatus(status);
        return room;
    }
}
