package com.emeraldgrove.dto;

import com.emeraldgrove.enums.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Article note payload for synchronization")
public record SyncArticleNoteRequest(
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$")
    @Schema(description = "External note identifier", example = "note-123")
    String id,

    @NotNull
    @Schema(description = "Note type", example = "QUESTION")
    NoteType type,

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Note content", example = "Why is partitioning needed here?")
    String content,

    @NotNull
    @Schema(description = "Client-side note creation timestamp", example = "1712160000000")
    Long createdAt
) {
}
