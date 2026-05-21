package com.emeraldgrove.dto.article;

import com.emeraldgrove.enums.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ArticleNoteDto(
    @NotBlank String id,
    @NotNull NoteType type,
    @NotBlank String content,
    Long createdAt
) {
}