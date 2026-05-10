package com.emeraldgrove.dto;

import java.util.List;

public record CollectionDto(
    Long id,
    String externalId,
    String name,
    Long createdAt,
    Long updatedAt,
    Integer articleCount,
    List<String> articleIds
) {
}
