package com.emeraldgrove.dto;

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
