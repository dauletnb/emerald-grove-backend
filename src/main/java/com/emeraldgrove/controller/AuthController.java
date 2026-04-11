package com.emeraldgrove.controller;

import com.emeraldgrove.dto.AuthResponse;
import com.emeraldgrove.dto.LoginRequest;
import com.emeraldgrove.dto.RefreshTokenRequest;
import com.emeraldgrove.dto.RegisterRequest;
import com.emeraldgrove.dto.UserDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /** Register a new user. Returns tokens immediately so the user is logged in. */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /** Login with email and password. Returns access + refresh tokens. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Exchange a refresh token for a new pair of tokens.
     * The old refresh token is revoked — this is called "token rotation".
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /** Logout: revoke the refresh token so it can no longer be used to get new access tokens. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Returns the current user's profile. Requires a valid access token. */
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        User user = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(new UserDto(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
