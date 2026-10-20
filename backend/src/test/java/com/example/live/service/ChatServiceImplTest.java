package com.example.live.service;

import com.example.live.common.constant.RoomStatus;
import com.example.live.common.exception.BizException;
import com.example.live.entity.LiveRoom;
import com.example.live.entity.User;
import com.example.live.mapper.ChatMessageMapper;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.mapper.UserMapper;
import com.example.live.service.impl.ChatServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMessageMapper chatMessageMapper;
    @Mock
    private LiveRoomMapper liveRoomMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(chatMessageMapper, liveRoomMapper, userMapper,
                redisTemplate, objectMapper, messagingTemplate);
    }

    @Test
    void rejectsSensitiveContentBeforeRoomLookup() {
        assertThatThrownBy(() -> chatService.sendChat(1L, 1L, "这里包含赌博内容"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("敏感词");
    }

    @Test
    void rejectsMessageWhenRoomIsNotLive() {
        when(liveRoomMapper.selectById(1L)).thenReturn(room(1L, RoomStatus.ENDED));

        assertThatThrownBy(() -> chatService.sendChat(1L, 1L, "hello"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("直播已结束");
    }

    @Test
    void rejectsMessagesWithinOneSecond() {
        when(liveRoomMapper.selectById(1L)).thenReturn(room(1L, RoomStatus.LIVE));
        when(userMapper.selectById(1L)).thenReturn(activeUser());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> chatService.sendChat(1L, 1L, "hello"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("发送太快");
    }

    private LiveRoom room(Long id, int status) {
        LiveRoom room = new LiveRoom();
        room.setId(id);
        room.setStatus(status);
        return room;
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setNickname("测试用户");
        user.setStatus(0);
        return user;
    }
}
