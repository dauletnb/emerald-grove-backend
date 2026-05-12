package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleDeletionSyncRequestDto;
import com.emeraldgrove.dto.CollectionLinkBatchSyncResponseDto;
import com.emeraldgrove.dto.CollectionLinkDeletionDto;
import com.emeraldgrove.dto.CollectionLinkSyncDto;
import com.emeraldgrove.dto.CollectionSyncDto;
import com.emeraldgrove.dto.ExternalIdDeletionRequestDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.dto.SyncBatchResponseDto;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.service.CollectionService;
import com.emeraldgrove.util.ControllerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
@Tag(name = "Sync", description = "API для фоновой синхронизации данных с клиентом")
public class SyncController {

    private final ArticleService articleService;
    private final CollectionService collectionService;
    private final ControllerUtil controllerUtil;

    @Operation(summary = "Синхронизация статьи")
    @PostMapping("/articles")
    public ResponseEntity<SyncArticleResponseDto> syncArticle(@Valid @RequestBody SyncArticleRequestDto request) {
        SyncArticleResponseDto response = articleService.syncArticle(request, controllerUtil.getCurrentUser());
        return ResponseEntity.status(response.status().toHttpStatus()).body(response);
    }

    @Operation(summary = "Синхронизировать удаленные статьи")
    @PostMapping("/articles/deletions")
    public ResponseEntity<SyncBatchResponseDto> syncDeletedArticles(@Valid @RequestBody ArticleDeletionSyncRequestDto request) {
        return ResponseEntity.ok(articleService.syncDeletedArticles(request, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать коллекции")
    @PostMapping("/collections")
    public ResponseEntity<SyncBatchResponseDto> syncCollections(@RequestBody List<@Valid CollectionSyncDto> collections) {
        return ResponseEntity.ok(collectionService.syncCollections(collections, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать удаленные коллекции")
    @PostMapping("/collections/deletions")
    public ResponseEntity<SyncBatchResponseDto> syncDeletedCollections(@Valid @RequestBody ExternalIdDeletionRequestDto request) {
        return ResponseEntity.ok(collectionService.syncDeletedCollections(request.externalIds(), controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать связи статей с коллекциями")
    @PostMapping("/collections/links")
    public ResponseEntity<CollectionLinkBatchSyncResponseDto> syncCollectionLinks(@RequestBody List<@Valid CollectionLinkSyncDto> links) {
        return ResponseEntity.ok(collectionService.syncCollectionLinks(links, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать удаленные связи статей с коллекциями")
    @PostMapping("/collections/links/deletions")
    public ResponseEntity<CollectionLinkBatchSyncResponseDto> syncDeletedCollectionLinks(
            @RequestBody List<@Valid CollectionLinkDeletionDto> links
    ) {
        return ResponseEntity.ok(collectionService.syncDeletedCollectionLinks(links, controllerUtil.getCurrentUser().getId()));
    }
}
