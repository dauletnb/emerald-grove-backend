package com.emeraldgrove.controller;

import com.emeraldgrove.api.ArticleSyncApi;
import com.emeraldgrove.dto.ArticleAiResponseDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.security.CurrentUserResolver;
import com.emeraldgrove.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
@Tag(name = "Articles", description = "API для управления синхронизацией статей, закладками и аналитикой на основе ИИ")
public class ArticleSyncController implements ArticleSyncApi {
    private final ArticleService articleService;
    private final CurrentUserResolver currentUserResolver;

    @Operation(summary = "Синхронизация статьи")
    @PostMapping("/sync")
    @Override
    public ResponseEntity<SyncArticleResponseDto> syncArticle(@Valid @RequestBody SyncArticleRequestDto request) {
        User user = currentUserResolver.getCurrentUser();
        SyncArticleResponseDto response = articleService.syncArticle(request, user);
        HttpStatus status = response.status() == SyncStatus.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @Operation(summary = "Получить все статьи")
    @GetMapping
    public ResponseEntity<List<ArticleSyncDto>> getArticles() {
        User user = currentUserResolver.getCurrentUser();
        return ResponseEntity.ok(articleService.getAll(user.getId()));
    }

    @Operation(summary = "Удалить статью")
    @DeleteMapping("/{externalId}")
    @Override
    public ResponseEntity<Void> deleteArticle(@PathVariable String externalId) {
        User user = currentUserResolver.getCurrentUser();
        articleService.deleteArticle(externalId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить результат анализа")
    @GetMapping("/{externalId}/ai")
    public ResponseEntity<ArticleAiResponseDto> getAiResult(@PathVariable String externalId) {
        User user = currentUserResolver.getCurrentUser();
        return ResponseEntity.ok(articleService.getAiResult(externalId, user.getId()));
    }

    @Operation(summary = "Повторить анализ")
    @PostMapping("/{externalId}/ai/retry")
    public ResponseEntity<Void> retryAiAnalysis(@PathVariable String externalId) {
        User user = currentUserResolver.getCurrentUser();
        articleService.retryAiAnalysis(externalId, user.getId());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "Удалить заметку")
    @DeleteMapping("/{externalId}/notes/{noteId}")
    @Override
    public ResponseEntity<Void> deleteNote(
        @PathVariable String externalId,
        @PathVariable String noteId
    ) {
        User user = currentUserResolver.getCurrentUser();
        articleService.deleteNote(externalId, noteId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
