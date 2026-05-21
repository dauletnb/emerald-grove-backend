package com.emeraldgrove.dto.sync;

import java.util.List;

public record SyncBatchResponseDto(
    int appliedCount,
    int skippedCount,
    List<SyncBatchItemResultDto> applied,
    List<SyncBatchItemResultDto> skipped
) {
}
