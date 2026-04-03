package com.emeraldgrove.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SyncArticleRequest(
        String externalId,
        @NotBlank String url,
        @NotBlank String title,
        String description,
        List<ArticleNoteDto> notes
) {}
