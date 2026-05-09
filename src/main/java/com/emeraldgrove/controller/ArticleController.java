package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleAiResponseDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.security.CurrentUserResolver;
import com.emeraldgrove.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "API для управления синхронизацией статей, закладками и аналитикой на основе ИИ")
public class ArticleController {
    private final ArticleService articleService;
    private final CurrentUserResolver currentUserResolver;

    @Operation(summary = "Синхронизация статьи")
    @PostMapping("/sync")
    public ResponseEntity<SyncArticleResponseDto> syncArticle(@Valid @RequestBody SyncArticleRequestDto request) {
        SyncArticleResponseDto response = articleService.syncArticle(request, getCurrentUser());
        return ResponseEntity.status(response.status().toHttpStatus()).body(response);
    }

    @Operation(summary = "Получить все статьи")
    @GetMapping
    public ResponseEntity<List<ArticleSyncDto>> getArticles() {
        return ResponseEntity.ok(articleService.getAll(getCurrentUser().getId()));
    }

    @Operation(summary = "Удалить статью")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable String externalId) {
        articleService.deleteArticle(externalId, getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить результат анализа")
    @GetMapping("/{externalId}/ai")
    public ResponseEntity<ArticleAiResponseDto> getAiResult(@PathVariable String externalId) {
        return ResponseEntity.ok(articleService.getAiResult(externalId, getCurrentUser().getId()));
    }

    @Operation(summary = "Повторить анализ")
    @PostMapping("/{externalId}/ai/retry")
    public ResponseEntity<Void> retryAiAnalysis(@PathVariable String externalId) {
        articleService.retryAiAnalysis(externalId, getCurrentUser().getId());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Удалить заметку")
    @DeleteMapping("/{externalId}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(
        @PathVariable String externalId,
        @PathVariable String noteId
    ) {
        articleService.deleteNote(externalId, noteId, getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        return currentUserResolver.getCurrentUser();
    }
}