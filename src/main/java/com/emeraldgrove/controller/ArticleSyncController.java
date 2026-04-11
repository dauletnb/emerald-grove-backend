package com.emeraldgrove.controller;

import com.emeraldgrove.api.ArticleSyncApi;
import com.emeraldgrove.dto.ArticleAiResponse;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.security.CurrentUserResolver;
import com.emeraldgrove.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleSyncController implements ArticleSyncApi {
    private final ArticleService articleService;
    private final CurrentUserResolver currentUserResolver;

    @PostMapping("/sync")
    @Override
    public ResponseEntity<SyncArticleResponse> syncArticle(@Valid @RequestBody SyncArticleRequest request) {
        User user = currentUserResolver.getCurrentUser();
        SyncArticleResponse response = articleService.syncArticle(request, user);
        HttpStatus status = response.status() == SyncStatus.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ArticleSyncDto>> getArticles() {
        User user = currentUserResolver.getCurrentUser();
        return ResponseEntity.ok(articleService.getAll(user.getId()));
    }

    @DeleteMapping("/{externalId}")
    @Override
    public ResponseEntity<Void> deleteArticle(@PathVariable String externalId) {
        User user = currentUserResolver.getCurrentUser();
        articleService.deleteArticle(externalId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{externalId}/ai")
    public ResponseEntity<ArticleAiResponse> getAiResult(@PathVariable String externalId) {
        User user = currentUserResolver.getCurrentUser();
        return ResponseEntity.ok(articleService.getAiResult(externalId, user.getId()));
    }

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
