package com.emeraldgrove.dto.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ExternalIdDeletionRequestDto(
    @NotEmpty List<@NotBlank String> externalIds
) {
}
