package com.emeraldgrove.dto;

import jakarta.validation.constraints.NotBlank;

public record SyncArticleRequest(
        String externalId,
        @NotBlank String url,
        @NotBlank String title,
        String description
) {}