package com.emeraldgrove.dto.sync;

import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.enums.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.Timestamp;

@Schema(description = "Synchronized article note snapshot")
public record SyncArticleNoteResponseDto(
    @Schema(description = "Client note identifier", example = "note-1")
    String id,
    @Schema(description = "Note type", example = "IDEA")
    NoteType type,
    @Schema(description = "Note content", example = "Remember this key detail")
    String content,
    @Schema(description = "Client note creation timestamp in milliseconds", example = "1712160000000")
    Long createdAt
) {
    public static SyncArticleNoteResponseDto fromEntity(ArticleNote note) {
        return new SyncArticleNoteResponseDto(
            note.getExternalId(),
            note.getType(),
            note.getContent(),
            toEpochMillis(note.getClientCreatedAt())
        );
    }

    private static Long toEpochMillis(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value).getTime();
    }
}
