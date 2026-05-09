package com.emeraldgrove.dto;

/**
 * Returned after successful login, registration, or token refresh.
 * The frontend stores accessToken and refreshToken in extension storage.
 */
public record AuthResponseDto(
    String accessToken,
    String refreshToken,
    UserInfo user
) {
    public record UserInfo(Long id, String email, String displayName) {}
}