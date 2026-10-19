package com.example.live.controller;

import com.example.live.common.result.Result;
import com.example.live.dto.UpdateProfileRequest;
import com.example.live.service.UserService;
import com.example.live.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "用户")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "当前用户资料")
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.currentUser());
    }

    @Operation(summary = "更新当前用户资料")
    @PutMapping("/me")
    public Result<UserVO> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(request));
    }

    @Operation(summary = "上传头像到 MinIO")
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserVO> uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Result.ok(userService.updateAvatar(file));
    }
}
