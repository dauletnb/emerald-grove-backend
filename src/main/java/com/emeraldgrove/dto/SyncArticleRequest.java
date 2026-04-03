package com.emeraldgrove.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for article synchronization")
public record SyncArticleRequest(
    @Schema(description = "External system identifier", example = "ext-12345")
    String externalId,
    @NotBlank
    @Schema(description = "Article source URL", example = "https://example.com/articles/emerald-grove")
    String url,
    @NotBlank
    @Schema(description = "Article title", example = "Emerald Grove launches new feature")
    String title,
    @Schema(description = "Short article description", example = "A short summary of the article content.")
    String description
) {
}