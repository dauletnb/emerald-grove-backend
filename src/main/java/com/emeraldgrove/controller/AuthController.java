package com.emeraldgrove.controller;

import com.emeraldgrove.dto.AuthResponseDto;
import com.emeraldgrove.dto.LoginRequestDto;
import com.emeraldgrove.dto.RefreshTokenRequestDto;
import com.emeraldgrove.dto.RegisterRequestDto;
import com.emeraldgrove.dto.UserDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "API для управления аутентификацией")
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "Регистрация пользователя. Возвращает токены доступа и обновления")
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Вход пользователя. Возвращает access + refresh токен")
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Обмен refresh токена на новую пару токенов")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponseDto> refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @Operation(summary = "Выход из системы")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить профиль текущего пользователя")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(new UserDto(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
