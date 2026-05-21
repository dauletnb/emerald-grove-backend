package com.emeraldgrove.dto.collection;

public record CollectionLinkSyncResultDto(
    String externalId,
    String articleExternalId,
    String collectionExternalId,
    String status,
    String reason
) {
}
