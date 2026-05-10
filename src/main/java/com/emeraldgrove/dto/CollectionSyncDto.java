package com.emeraldgrove.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionSyncDto(
    @NotBlank
    @Size(max = 36)
    String externalId,
    @NotBlank
    @Size(max = 255)
    String name
) {
}
