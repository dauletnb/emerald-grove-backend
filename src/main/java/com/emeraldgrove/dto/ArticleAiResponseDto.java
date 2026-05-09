package com.emeraldgrove.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record ArticleAiResponseDto(
    String aiStatus,
    @JsonRawValue String aiResult
) {
}
