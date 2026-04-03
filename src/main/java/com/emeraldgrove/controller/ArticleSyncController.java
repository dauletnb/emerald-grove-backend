package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
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
        return ResponseEntity.ok(articleService.sync(request));
    }

    @GetMapping
    public ResponseEntity<java.util.List<ArticleSyncDto>> getArticles() {
        return ResponseEntity.status(HttpStatus.OK).body(articleService.getAll());
    }
}
