package com.example.live.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.live.common.constant.RoomStatus;
import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.entity.ChatMessage;
import com.example.live.entity.LiveRoom;
import com.example.live.entity.User;
import com.example.live.mapper.ChatMessageMapper;
import com.example.live.mapper.LiveRoomMapper;
import com.example.live.mapper.UserMapper;
import com.example.live.service.ChatService;
import com.example.live.vo.ChatMessageVO;
import com.example.live.ws.WsMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final int MAX_CONTENT_LENGTH = 100;
    private static final int MAX_HISTORY = 100;
    private static final Duration CHAT_CACHE_TTL = Duration.ofHours(6);
    private static final Set<String> SENSITIVE_WORDS = Set.of("赌博", "诈骗", "外挂", "色情");

    private final ChatMessageMapper chatMessageMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatServiceImpl(ChatMessageMapper chatMessageMapper, LiveRoomMapper liveRoomMapper,
                           UserMapper userMapper, StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.chatMessageMapper = chatMessageMapper;
        this.liveRoomMapper = liveRoomMapper;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendChat(Long roomId, Long userId, String rawContent) {
        String content = rawContent == null ? "" : rawContent.trim();
        if (!StringUtils.hasText(content)) {
            throw new BizException(ResultCode.BAD_REQUEST, "消息不能为空");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BizException(ResultCode.BAD_REQUEST, "消息最多 100 个字符");
        }
        if (SENSITIVE_WORDS.stream().anyMatch(content::contains)) {
            throw new BizException(ResultCode.BAD_REQUEST, "消息包含敏感词");
        }

        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ResultCode.NOT_FOUND, "直播间不存在");
        }
        if (!Integer.valueOf(RoomStatus.LIVE).equals(room.getStatus())) {
            throw new BizException(ResultCode.CONFLICT, "直播已结束");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁");
        }
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "你已被禁言");
        }

        String rateKey = "chat:rate:" + userId;
        Boolean first = redisTemplate.opsForValue().setIfAbsent(rateKey, "1", Duration.ofSeconds(1));
        if (Boolean.FALSE.equals(first)) {
            throw new BizException(ResultCode.TOO_MANY_REQUESTS, "发送太快，请稍后再试");
        }

        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setContent(content);
        message.setType(0);
        chatMessageMapper.insert(message);
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }

        ChatMessageVO vo = toVO(message, user);
        cacheMessage(roomId, vo);
        broadcast(roomId, WsMessage.of("CHAT", vo));
    }

    @Override
    public List<ChatMessageVO> history(Long roomId, int limit) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BizException(ResultCode.NOT_FOUND, "直播间不存在");
        }
        int size = Math.max(1, Math.min(limit, MAX_HISTORY));
        String key = historyKey(roomId);
        try {
            List<String> cached = redisTemplate.opsForList().range(key, 0, -1);
            List<ChatMessageVO> messages = new ArrayList<>();
            if (cached != null) {
                for (String json : cached) {
                    try {
                        messages.add(objectMapper.readValue(json, ChatMessageVO.class));
                    } catch (JsonProcessingException ignored) {
                        // 单条缓存损坏时跳过，继续读后面的消息
                    }
                }
            }
            if (!messages.isEmpty()) {
                return messages;
            }
        } catch (Exception ex) {
            log.warn("read chat history from Redis failed, fallback to DB, roomId={}", roomId, ex);
        }

        List<ChatMessage> latest = chatMessageMapper.selectList(
                Wrappers.<ChatMessage>lambdaQuery()
                        .eq(ChatMessage::getRoomId, roomId)
                        .orderByDesc(ChatMessage::getId)
                        .last("LIMIT " + size));
        Collections.reverse(latest);
        List<ChatMessageVO> result = new ArrayList<>(latest.size());
        for (ChatMessage message : latest) {
            User user = userMapper.selectById(message.getUserId());
            result.add(toVO(message, user));
        }
        cacheMessages(roomId, result);
        return result;
    }

    private ChatMessageVO toVO(ChatMessage message, User user) {
        return new ChatMessageVO(
                message.getId(),
                message.getRoomId(),
                message.getUserId(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getAvatar(),
                message.getContent(),
                message.getType(),
                message.getCreatedAt());
    }

    private void cacheMessage(Long roomId, ChatMessageVO message) {
        try {
            String key = historyKey(roomId);
            redisTemplate.opsForList().rightPush(key, objectMapper.writeValueAsString(message));
            redisTemplate.opsForList().trim(key, -MAX_HISTORY, -1);
            redisTemplate.expire(key, CHAT_CACHE_TTL);
        } catch (Exception ex) {
            log.warn("cache chat message failed, roomId={}", roomId, ex);
        }
    }

    private void cacheMessages(Long roomId, List<ChatMessageVO> messages) {
        if (messages.isEmpty()) {
            return;
        }
        try {
            String key = historyKey(roomId);
            List<String> values = new ArrayList<>(messages.size());
            for (ChatMessageVO message : messages) {
                values.add(objectMapper.writeValueAsString(message));
            }
            redisTemplate.delete(key);
            redisTemplate.opsForList().rightPushAll(key, values);
            redisTemplate.opsForList().trim(key, -MAX_HISTORY, -1);
            redisTemplate.expire(key, CHAT_CACHE_TTL);
        } catch (Exception ex) {
            log.warn("cache chat history failed, roomId={}", roomId, ex);
        }
    }

    private void broadcast(Long roomId, WsMessage<?> message) {
        try {
            messagingTemplate.convertAndSend("/topic/rooms/" + roomId, message);
        } catch (Exception ex) {
            log.warn("broadcast chat failed, roomId={}", roomId, ex);
        }
    }

    private String historyKey(Long roomId) {
        return "live:chat:" + roomId;
    }
}
