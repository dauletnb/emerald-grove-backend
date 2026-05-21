package com.emeraldgrove.dto.sync;

public record SyncBatchItemResultDto(
    String externalId,
    String status,
    String reason
) {
}
