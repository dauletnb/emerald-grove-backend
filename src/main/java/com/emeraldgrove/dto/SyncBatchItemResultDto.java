package com.emeraldgrove.dto;

public record SyncBatchItemResultDto(
    String externalId,
    String status,
    String reason
) {
}
