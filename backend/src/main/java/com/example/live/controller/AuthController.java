package com.example.live.controller;

import com.example.live.common.result.Result;
import com.example.live.dto.LoginRequest;
import com.example.live.dto.LogoutRequest;
import com.example.live.dto.RefreshRequest;
import com.example.live.dto.RegisterRequest;
import com.example.live.service.AuthService;
import com.example.live.vo.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "注册并自动登录")
    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    @Operation(summary = "刷新令牌（refreshToken 轮换）")
    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.ok(authService.refresh(request));
    }

    @Operation(summary = "登出（删除服务端 refreshToken）")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody(required = false) LogoutRequest request) {
        authService.logout(request);
        return Result.ok();
    }
}
