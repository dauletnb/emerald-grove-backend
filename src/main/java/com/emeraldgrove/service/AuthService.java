package com.emeraldgrove.service;

import com.emeraldgrove.dto.AuthResponse;
import com.emeraldgrove.dto.LoginRequest;
import com.emeraldgrove.dto.RegisterRequest;
import com.emeraldgrove.entity.User;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);
    User getCurrentUser(String email);
}
