package com.emeraldgrove.dto;

import java.util.List;

public record CollectionLinkBatchSyncResponseDto(
    int appliedCount,
    int skippedCount,
    List<CollectionLinkSyncResultDto> applied,
    List<CollectionLinkSyncResultDto> skipped
) {
}
