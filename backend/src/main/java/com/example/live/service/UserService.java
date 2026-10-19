package com.example.live.service;

import com.example.live.dto.UpdateProfileRequest;
import com.example.live.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserVO currentUser();

    UserVO updateProfile(UpdateProfileRequest request);

    UserVO updateAvatar(MultipartFile file);
}
