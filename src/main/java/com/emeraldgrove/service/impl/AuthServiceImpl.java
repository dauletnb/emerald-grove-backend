package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.AuthResponse;
import com.emeraldgrove.dto.LoginRequest;
import com.emeraldgrove.dto.RegisterRequest;
import com.emeraldgrove.entity.RefreshToken;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.exception.AuthException;
import com.emeraldgrove.repository.RefreshTokenRepository;
import com.emeraldgrove.repository.UserRepository;
import com.emeraldgrove.security.JwtService;
import com.emeraldgrove.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${emerald-grove.security.refresh-token-expiration-days:7}")
    private int refreshTokenExpirationDays;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException("Email is already registered.", HttpStatus.CONFLICT);
        }

        String displayName = (request.displayName() != null && !request.displayName().isBlank())
            ? request.displayName().trim()
            : extractNameFromEmail(email);

        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.password()))
            .displayName(displayName)
            .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException("Invalid email or password.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new AuthException("Invalid refresh token.", HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            throw new AuthException("Refresh token has been revoked.", HttpStatus.UNAUTHORIZED);
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException("Refresh token has expired.", HttpStatus.UNAUTHORIZED);
        }

        // Token rotation: revoke the used token and issue a new one
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
            .orElseThrow(() -> new AuthException("User not found.", HttpStatus.UNAUTHORIZED));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(refreshTokenValue)
            .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
            .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken,
            refreshTokenValue,
            new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getDisplayName())
        );
    }

    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
}
