package com.emeraldgrove.dto;

import java.util.List;

public record ArticleSyncDto(
    Long articleId,
    String externalId,
    String url,
    String title,
    String description,
    String content,
    boolean isFavorite,
    boolean isReadLater,
    Long createdAt,
    Long updatedAt,
    String aiStatus,
    List<ArticleNoteDto> notes
) {
}
