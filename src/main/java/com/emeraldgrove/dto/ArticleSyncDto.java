package com.emeraldgrove.dto;

import java.util.List;

public record ArticleSyncDto(
        Long articleId,
        String externalId,
        String url,
        String title,
        String description,
        Boolean isRead,
        Long createdAt,
        Long updatedAt,
        List<ArticleNoteDto> notes
) {
}
