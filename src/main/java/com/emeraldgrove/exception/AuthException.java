package com.emeraldgrove.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when authentication or authorization fails in the auth service.
 * Maps to 401 Unauthorized or 409 Conflict depending on the status provided.
 */
public class AuthException extends RuntimeException {

    private final HttpStatus status;

    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
