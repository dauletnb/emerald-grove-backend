package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.AuthResponseDto;
import com.emeraldgrove.dto.LoginRequestDto;
import com.emeraldgrove.dto.RegisterRequestDto;
import com.emeraldgrove.entity.RefreshToken;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.exception.AuthException;
import com.emeraldgrove.repository.RefreshTokenRepository;
import com.emeraldgrove.repository.UserRepository;
import com.emeraldgrove.security.JwtService;
import com.emeraldgrove.constants.AuthConstants;
import com.emeraldgrove.constants.ErrorMessages;
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
    public AuthResponseDto register(RegisterRequestDto request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException(ErrorMessages.ERROR_EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT);
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
    public AuthResponseDto login(LoginRequestDto request) {
        String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthException(ErrorMessages.ERROR_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthException(ErrorMessages.ERROR_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponseDto refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
            .orElseThrow(() -> new AuthException(ErrorMessages.ERROR_INVALID_REFRESH_TOKEN, HttpStatus.UNAUTHORIZED));

        if (stored.isRevoked()) {
            throw new AuthException(ErrorMessages.ERROR_REFRESH_TOKEN_REVOKED, HttpStatus.UNAUTHORIZED);
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AuthException(ErrorMessages.ERROR_REFRESH_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
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
            .orElseThrow(() -> new AuthException(ErrorMessages.ERROR_USER_NOT_FOUND, HttpStatus.UNAUTHORIZED));
    }

    private AuthResponseDto buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
            .user(user)
            .token(refreshTokenValue)
            .expiresAt(LocalDateTime.now().plusDays(refreshTokenExpirationDays))
            .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthResponseDto(
            accessToken,
            refreshTokenValue,
            new AuthResponseDto.UserInfo(user.getId(), user.getEmail(), user.getDisplayName())
        );
    }

    private String extractNameFromEmail(String email) {
        int atIndex = email.indexOf(AuthConstants.EMAIL_SEPARATOR);
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
}
