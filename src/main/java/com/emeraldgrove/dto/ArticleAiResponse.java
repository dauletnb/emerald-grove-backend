package com.emeraldgrove.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

public record ArticleAiResponse(
    String aiStatus,
    @JsonRawValue String aiResult
) {
}
