package com.emeraldgrove.dto.collection;

import java.util.List;

public record CollectionLinkBatchSyncResponseDto(
    int appliedCount,
    int skippedCount,
    List<CollectionLinkSyncResultDto> applied,
    List<CollectionLinkSyncResultDto> skipped
) {
}
