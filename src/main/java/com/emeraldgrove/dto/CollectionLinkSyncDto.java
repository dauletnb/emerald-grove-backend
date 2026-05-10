package com.emeraldgrove.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionLinkSyncDto(
    @NotBlank
    @Size(max = 64)
    String externalId,
    @NotBlank
    @Size(max = 36)
    String articleExternalId,
    @NotBlank
    @Size(max = 36)
    String collectionExternalId,
    Long clientCreatedAt
) {
}
