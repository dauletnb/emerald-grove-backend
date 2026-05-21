package com.emeraldgrove.dto.collection;

import jakarta.validation.constraints.NotBlank;

public record CollectionLinkDeletionDto(
    @NotBlank String externalId,
    @NotBlank String articleExternalId,
    @NotBlank String collectionExternalId
) {
}
