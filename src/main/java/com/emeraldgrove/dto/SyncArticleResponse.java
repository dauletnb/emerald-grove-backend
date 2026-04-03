package com.emeraldgrove.dto;

import com.emeraldgrove.enums.SyncStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Result of article synchronization")
public record SyncArticleResponse(
    @Schema(description = "Synchronization result", example = "CREATED")
    SyncStatus status,
    @Schema(description = "Persisted article identifier", example = "42")
    Long articleId
) {
}