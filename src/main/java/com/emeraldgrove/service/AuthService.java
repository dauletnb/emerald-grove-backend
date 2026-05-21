package com.emeraldgrove.service;

import com.emeraldgrove.dto.auth.AuthResponseDto;
import com.emeraldgrove.dto.auth.LoginRequestDto;
import com.emeraldgrove.dto.auth.RegisterRequestDto;
import com.emeraldgrove.entity.User;

public interface AuthService {
    AuthResponseDto register(RegisterRequestDto request);
    AuthResponseDto login(LoginRequestDto request);
    AuthResponseDto refresh(String refreshToken);
    void logout(String refreshToken);
    User getCurrentUser(String email);
}
