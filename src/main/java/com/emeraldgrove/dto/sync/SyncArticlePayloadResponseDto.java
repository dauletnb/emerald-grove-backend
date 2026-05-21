package com.emeraldgrove.dto.sync;

import com.emeraldgrove.entity.Article;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Synchronized article snapshot")
public record SyncArticlePayloadResponseDto(
    @Schema(description = "Persisted article identifier", example = "42")
    Long articleId,
    @Schema(description = "Client article identifier", example = "frontend-article-123")
    String externalId,
    @Schema(description = "Article URL", example = "https://example.com/article")
    String url,
    @Schema(description = "Article title", example = "Interesting article")
    String title,
    @Schema(description = "Article description", example = "Short description")
    String description,
    @Schema(description = "Full article content (HTML from Readability)")
    String content,
    @Schema(description = "Whether the article is marked as favorite", example = "true")
    boolean isFavorite,
    @Schema(description = "Whether the article is marked to read later", example = "false")
    boolean isReadLater,
    @Schema(description = "Server creation timestamp in milliseconds", example = "1712160000000")
    Long createdAt,
    @Schema(description = "Server update timestamp in milliseconds", example = "1712160005000")
    Long updatedAt,
    @Schema(description = "AI processing status", example = "PENDING")
    String aiStatus,
    @Schema(description = "Synchronized notes")
    List<SyncArticleNoteResponseDto> notes
) {
    public static SyncArticlePayloadResponseDto fromEntity(Article article) {
        return new SyncArticlePayloadResponseDto(
            article.getId(),
            article.getExternalId(),
            article.getUrl(),
            article.getTitle(),
            article.getDescription(),
            article.getContent(),
            article.isFavorite(),
            article.isReadLater(),
            toEpochMillis(article.getCreatedAt()),
            toEpochMillis(article.getUpdatedAt()),
            article.getAiStatus(),
            article.getNotes().stream()
                .map(SyncArticleNoteResponseDto::fromEntity)
                .toList()
        );
    }

    private static Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value).getTime();
    }
}
