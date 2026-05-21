package com.emeraldgrove.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDto(@NotBlank String refreshToken) {}