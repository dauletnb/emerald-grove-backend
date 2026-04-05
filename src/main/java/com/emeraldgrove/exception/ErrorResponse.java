package com.emeraldgrove.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.Instant;

@Schema(description = "Standard API error payload")
public record ErrorResponse(
    @Schema(description = "Machine-readable error code", example = "VALIDATION_ERROR")
    String code,
    @Schema(description = "Human-readable error message", example = "title: must not be blank")
    String message,
    @Schema(description = "HTTP status code", example = "400")
    int status,
    @Schema(description = "Error timestamp in UTC", example = "2026-04-03T16:21:45.123Z")
    Instant timestamp
) {
    public static ErrorResponse of(String code, String message, HttpStatus status) {
        return new ErrorResponse(code, message, status.value(), Instant.now());
    }
}