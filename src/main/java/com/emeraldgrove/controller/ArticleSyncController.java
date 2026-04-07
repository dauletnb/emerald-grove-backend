package com.emeraldgrove.controller;

import com.emeraldgrove.api.ArticleSyncApi;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class ArticleSyncController implements ArticleSyncApi {
    private final ArticleService articleService;

    @PostMapping("/sync")
    @Override
    public ResponseEntity<SyncArticleResponse> syncArticle(@Valid @RequestBody SyncArticleRequest request) {
        SyncArticleResponse response = articleService.syncArticle(request);
        HttpStatus status = response.status() == SyncStatus.CREATED ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ArticleSyncDto>> getArticles() {
        return ResponseEntity.status(HttpStatus.OK).body(articleService.getAll());
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> deleteArticle(@PathVariable String externalId) {
        articleService.deleteArticle(externalId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{externalId}/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(
        @PathVariable String externalId,
        @PathVariable String noteId
    ) {
        articleService.deleteNote(externalId, noteId);
        return ResponseEntity.noContent().build();
    }
}
