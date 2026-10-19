package com.example.live.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.common.util.JwtUtil;
import com.example.live.config.JwtProperties;
import com.example.live.converter.UserConverter;
import com.example.live.dto.LoginRequest;
import com.example.live.dto.LogoutRequest;
import com.example.live.dto.RefreshRequest;
import com.example.live.dto.RegisterRequest;
import com.example.live.entity.User;
import com.example.live.mapper.UserMapper;
import com.example.live.service.AuthService;
import com.example.live.vo.AuthResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String REFRESH_KEY_PREFIX = "auth:refresh:";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Redis 5 不支持 GETDEL，用 Lua 保证「取出即删除」的原子性，避免刷新令牌被重复使用。
     */
    private static final DefaultRedisScript<String> CONSUME_REFRESH_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if value then redis.call('DEL', KEYS[1]) end; "
                    + "return value",
            String.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;

    public AuthServiceImpl(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                           StringRedisTemplate stringRedisTemplate, JwtProperties jwtProperties) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        Long existing = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (existing != null && existing > 0) {
            throw new BizException(ResultCode.CONFLICT, "用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setNickname(StringUtils.hasText(request.nickname()) ? request.nickname().trim() : username);
        user.setRole("USER");
        user.setStatus(0);
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 并发注册或逻辑删除的历史用户名都会命中唯一键
            throw new BizException(ResultCode.CONFLICT, "用户名已存在");
        }
        return issueTokens(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁");
        }
        return issueTokens(user);
    }

    @Override
    public AuthResponse refresh(RefreshRequest request) {
        String userIdValue = stringRedisTemplate.execute(
                CONSUME_REFRESH_SCRIPT, List.of(refreshKey(request.refreshToken())));
        if (!StringUtils.hasText(userIdValue)) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }

        User user;
        try {
            user = userMapper.selectById(Long.valueOf(userIdValue));
        } catch (NumberFormatException ex) {
            throw new BizException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
        }
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁");
        }
        return issueTokens(user);
    }

    @Override
    public void logout(LogoutRequest request) {
        if (request != null && StringUtils.hasText(request.refreshToken())) {
            stringRedisTemplate.delete(refreshKey(request.refreshToken()));
        }
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = generateRefreshToken();
        stringRedisTemplate.opsForValue().set(
                refreshKey(refreshToken),
                String.valueOf(user.getId()),
                Duration.ofSeconds(jwtProperties.refreshExpireSeconds()));
        return new AuthResponse(accessToken, refreshToken, jwtUtil.getAccessExpireSeconds(),
                UserConverter.toVO(user));
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String refreshKey(String refreshToken) {
        return REFRESH_KEY_PREFIX + refreshToken;
    }
}
