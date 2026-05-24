package com.emeraldgrove.controller;

import com.emeraldgrove.dto.article.UpdateNoteRequestDto;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.util.ControllerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/{articleExternalId}/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "API для управления заметками к статьям")
public class NoteController {
    private final ArticleService articleService;
    private final ControllerUtil controllerUtil;

    @Operation(summary = "Обновить заметку")
    @PutMapping("/{noteId}")
    public ResponseEntity<Void> updateNote(@PathVariable String articleExternalId,
                                           @PathVariable String noteId,
                                           @Valid @RequestBody UpdateNoteRequestDto request) {
        articleService.updateNote(articleExternalId, noteId, request.content(), request.type(), controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Удалить заметку")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String articleExternalId, @PathVariable String noteId) {
        articleService.deleteNote(articleExternalId, noteId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }
}