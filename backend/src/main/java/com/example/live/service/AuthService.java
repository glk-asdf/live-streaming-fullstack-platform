package com.example.live.service;

import com.example.live.dto.LoginRequest;
import com.example.live.dto.LogoutRequest;
import com.example.live.dto.RefreshRequest;
import com.example.live.dto.RegisterRequest;
import com.example.live.vo.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);

    void logout(LogoutRequest request);
}
