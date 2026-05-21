package com.emeraldgrove.dto.collection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CollectionRequestDto(
    @NotBlank
    @Size(max = 255)
    String name
) {
}
