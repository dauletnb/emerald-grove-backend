package com.emeraldgrove.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

import java.util.List;

public record SyncArticleRequest(
    @Size(max = 36)
    String externalId,
    @NotBlank
    @Size(max = 2048)
    String url,
    @NotBlank
    @Size(max = 255)
    String title,
    @Size(max = 5000)
    String description,
    @Valid List<SyncArticleNoteRequest> notes
) {
}