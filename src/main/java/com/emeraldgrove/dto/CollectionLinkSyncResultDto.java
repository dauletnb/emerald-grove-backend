package com.emeraldgrove.dto;

public record CollectionLinkSyncResultDto(
    String externalId,
    String articleExternalId,
    String collectionExternalId,
    String status,
    String reason
) {
}
