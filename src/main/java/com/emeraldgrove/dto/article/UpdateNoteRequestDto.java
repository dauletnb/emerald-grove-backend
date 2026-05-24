package com.emeraldgrove.dto.article;

import com.emeraldgrove.enums.NoteType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to update an existing note")
public record UpdateNoteRequestDto(
    @NotNull
    @Schema(description = "Note type", example = "QUESTION")
    NoteType type,

    @NotBlank
    @Size(max = 2000)
    @Schema(description = "Updated note content", example = "Updated note text")
    String content
) {
}
