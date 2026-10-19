package com.example.live.converter;

import com.example.live.entity.User;
import com.example.live.vo.UserVO;

public final class UserConverter {

    private UserConverter() {
    }

    public static UserVO toVO(User user) {
        return new UserVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar(),
                user.getBio(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
