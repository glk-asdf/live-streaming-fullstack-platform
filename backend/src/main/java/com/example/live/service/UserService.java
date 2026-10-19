package com.example.live.service;

import com.example.live.dto.UpdateProfileRequest;
import com.example.live.vo.UserVO;

public interface UserService {

    UserVO currentUser();

    UserVO updateProfile(UpdateProfileRequest request);
}
