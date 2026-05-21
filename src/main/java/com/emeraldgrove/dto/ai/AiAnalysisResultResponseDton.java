package com.emeraldgrove.dto.ai;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record AiAnalysisResultResponseDton(
    String aiStatus,
    @JsonRawValue String aiResult
) {
}
