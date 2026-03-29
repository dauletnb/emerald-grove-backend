package com.emeraldgrove.exception;

import org.springframework.http.HttpStatus;

import java.time.Instant;

// ErrorResponse.java
public record ErrorResponse(
        String code,        // машиночитаемый код: "DUPLICATE", "NOT_FOUND" и тд
        String message,     // человекочитаемое сообщение
        int status,         // HTTP статус код: 409, 404 и тд
        Instant timestamp   // время ошибки — полезно при дебаггинге
) {
    // фабричный метод чтобы не писать new ErrorResponse(...) каждый раз
    public static ErrorResponse of(String code, String message, HttpStatus status) {
        return new ErrorResponse(code, message, status.value(), Instant.now());
    }
}
