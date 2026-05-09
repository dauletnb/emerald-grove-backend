package com.emeraldgrove.enums;

import org.springframework.http.HttpStatus;

public enum SyncStatus {
    CREATED,
    UPDATED;

    public HttpStatus toHttpStatus() {
        return this == CREATED ? HttpStatus.CREATED : HttpStatus.OK;
    }
}
