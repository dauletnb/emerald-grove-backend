package com.emeraldgrove.controller;

import com.emeraldgrove.api.ArticleSyncApi;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}