package com.example.live.service.impl;

import com.example.live.common.exception.BizException;
import com.example.live.common.result.ResultCode;
import com.example.live.common.util.SecurityUtil;
import com.example.live.converter.UserConverter;
import com.example.live.dto.UpdateProfileRequest;
import com.example.live.entity.User;
import com.example.live.mapper.UserMapper;
import com.example.live.service.UserService;
import com.example.live.vo.UserVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserVO currentUser() {
        return UserConverter.toVO(loadCurrentUser());
    }

    @Override
    public UserVO updateProfile(UpdateProfileRequest request) {
        User user = loadCurrentUser();

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (!StringUtils.hasText(nickname)) {
                throw new BizException(ResultCode.BAD_REQUEST, "昵称不能为空");
            }
            user.setNickname(nickname);
        }
        if (request.avatar() != null) {
            String avatar = request.avatar().trim();
            user.setAvatar(StringUtils.hasText(avatar) ? avatar : null);
        }
        if (request.bio() != null) {
            String bio = request.bio().trim();
            user.setBio(StringUtils.hasText(bio) ? bio : null);
        }

        userMapper.updateById(user);
        return UserConverter.toVO(user);
    }

    private User loadCurrentUser() {
        User user = userMapper.selectById(SecurityUtil.currentUserId());
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在，请重新登录");
        }
        if (Integer.valueOf(2).equals(user.getStatus())) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已被封禁");
        }
        return user;
    }
}
