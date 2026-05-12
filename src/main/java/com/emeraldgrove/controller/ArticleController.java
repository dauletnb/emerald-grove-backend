package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.service.CollectionService;
import com.emeraldgrove.util.ControllerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "API для управления статьями и закладками")
public class ArticleController {
    private final ArticleService articleService;
    private final CollectionService collectionService;
    private final ControllerUtil controllerUtil;

    @Operation(summary = "Получить все статьи")
    @GetMapping
    public ResponseEntity<List<ArticleSyncDto>> getArticles() {
        return ResponseEntity.ok(articleService.getAll(controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Удалить статью")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable String externalId) {
        articleService.deleteArticle(externalId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Удалить заметку")
    @DeleteMapping("/{externalId}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable String externalId, @PathVariable String noteId) {
        articleService.deleteNote(externalId, noteId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить ID коллекций статьи")
    @GetMapping("/{articleExternalId}/collections")
    public ResponseEntity<List<String>> getArticleCollectionIds(@PathVariable String articleExternalId) {
        return ResponseEntity.ok(collectionService.getArticleCollectionIds(articleExternalId, controllerUtil.getCurrentUser().getId()));
    }
}
