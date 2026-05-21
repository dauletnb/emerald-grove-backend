package com.emeraldgrove.controller;

import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.util.ControllerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles/{articleExternalId}/notes")
@RequiredArgsConstructor
@Tag(name = "Notes", description = "API для управления заметками к статьям")
public class NoteController {
    private final ArticleService articleService;
    private final ControllerUtil controllerUtil;

    @Operation(summary = "Удалить заметку")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String articleExternalId, @PathVariable String noteId) {
        articleService.deleteNote(articleExternalId, noteId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }
}