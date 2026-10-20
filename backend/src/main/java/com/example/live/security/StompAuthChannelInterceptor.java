package com.example.live.security;

import com.example.live.common.util.JwtUtil;
import com.example.live.entity.User;
import com.example.live.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * STOMP CONNECT 帧鉴权：浏览器不能给 WebSocket 握手设置自定义头，
 * 因此 accessToken 放在 CONNECT 帧的 Authorization native header。
 * 游客允许连接和订阅，但 SEND 只允许登录用户（心跳除外）。
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public StompAuthChannelInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SEND.equals(accessor.getCommand())
                && !isAuthenticated(accessor)
                && !isHeartbeat(accessor.getDestination())) {
            throw new AccessDeniedException("请先登录后再发言");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return;
        }
        if (!authorization.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("无效的登录令牌");
        }
        try {
            Claims claims = jwtUtil.parse(authorization.substring(BEARER_PREFIX.length()));
            Long userId = Long.valueOf(claims.getSubject());
            User user = userMapper.selectById(userId);
            if (user == null || Integer.valueOf(2).equals(user.getStatus())) {
                throw new MessagingException("账号不存在或已被封禁");
            }
            LoginUser loginUser = new LoginUser(user.getId(), user.getUsername(), user.getRole());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
            accessor.setUser(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new MessagingException("无效的登录令牌");
        }
    }

    private boolean isAuthenticated(StompHeaderAccessor accessor) {
        return accessor.getUser() instanceof UsernamePasswordAuthenticationToken authentication
                && authentication.getPrincipal() instanceof LoginUser;
    }

    private boolean isHeartbeat(String destination) {
        return destination != null
                && destination.startsWith("/app/rooms/")
                && destination.endsWith("/heartbeat");
    }
}
