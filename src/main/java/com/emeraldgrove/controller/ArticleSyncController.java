package com.emeraldgrove.controller;

import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleSyncController {

    private final ArticleService articleService;

    @PostMapping("/sync")
    public ResponseEntity<SyncArticleResponse> syncArticle(@Valid @RequestBody SyncArticleRequest request) {
        SyncArticleResponse response = articleService.sync(request);
        if (response.status() == SyncStatus.DUPLICATE) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }
}