package com.emeraldgrove.controller;

import com.emeraldgrove.dto.CollectionDto;
import com.emeraldgrove.dto.CollectionLinkBatchSyncResponseDto;
import com.emeraldgrove.dto.CollectionLinkDeletionDto;
import com.emeraldgrove.dto.CollectionLinkSyncDto;
import com.emeraldgrove.dto.CollectionRequestDto;
import com.emeraldgrove.dto.CollectionSyncDto;
import com.emeraldgrove.dto.ExternalIdDeletionRequestDto;
import com.emeraldgrove.dto.SyncBatchResponseDto;
import com.emeraldgrove.util.ControllerUtil;
import com.emeraldgrove.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "API для управления пользовательскими коллекциями")
public class CollectionController {
    private final CollectionService collectionService;
    private final ControllerUtil controllerUtil;

    @Operation(summary = "Создать коллекцию")
    @PostMapping
    public ResponseEntity<CollectionDto> createCollection(@Valid @RequestBody CollectionRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(collectionService.createCollection(request, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Переименовать коллекцию")
    @PutMapping("/{externalId}")
    public ResponseEntity<CollectionDto> renameCollection(
        @PathVariable String externalId,
        @Valid @RequestBody CollectionRequestDto request
    ) {
        return ResponseEntity.ok(collectionService.renameCollection(externalId, request, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Удалить коллекцию")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> deleteCollection(@PathVariable String externalId) {
        collectionService.deleteCollection(externalId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить все коллекции пользователя")
    @GetMapping
    public ResponseEntity<List<CollectionSyncDto>> getAllCollections() {
        return ResponseEntity.ok(collectionService.getAllCollections(controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Получить коллекцию пользователя")
    @GetMapping("/{externalId}")
    public ResponseEntity<CollectionDto> getCollection(@PathVariable String externalId) {
        return ResponseEntity.ok(collectionService.getCollection(externalId, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Добавить статью в коллекцию")
    @PostMapping("/{collectionExternalId}/articles/{articleExternalId}")
    public ResponseEntity<Void> addArticleToCollection(
        @PathVariable String collectionExternalId,
        @PathVariable String articleExternalId
    ) {
        collectionService.addArticleToCollection(articleExternalId, collectionExternalId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Удалить статью из коллекции")
    @DeleteMapping("/{collectionExternalId}/articles/{articleExternalId}")
    public ResponseEntity<Void> removeArticleFromCollection(
        @PathVariable String collectionExternalId,
        @PathVariable String articleExternalId
    ) {
        collectionService.removeArticleFromCollection(articleExternalId, collectionExternalId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить ID статей в коллекции")
    @GetMapping("/{externalId}/articles")
    public ResponseEntity<List<String>> getCollectionArticleIds(@PathVariable String externalId) {
        return ResponseEntity.ok(collectionService.getCollectionArticleIds(externalId, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать коллекции")
    @PostMapping("/sync")
    public ResponseEntity<SyncBatchResponseDto> syncCollections(@RequestBody List<@Valid CollectionSyncDto> collections) {
        return ResponseEntity.ok(collectionService.syncCollections(collections, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать связи статей с коллекциями")
    @PostMapping("/links/sync")
    public ResponseEntity<CollectionLinkBatchSyncResponseDto> syncCollectionLinks(@RequestBody List<@Valid CollectionLinkSyncDto> links) {
        return ResponseEntity.ok(collectionService.syncCollectionLinks(links, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать удаленные коллекции")
    @PostMapping("/sync/deletions")
    public ResponseEntity<SyncBatchResponseDto> syncDeletedCollections(@Valid @RequestBody ExternalIdDeletionRequestDto request) {
        return ResponseEntity.ok(collectionService.syncDeletedCollections(request.externalIds(), controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Синхронизировать удаленные связи статей с коллекциями")
    @PostMapping("/links/sync/deletions")
    public ResponseEntity<CollectionLinkBatchSyncResponseDto> syncDeletedCollectionLinks(
        @RequestBody List<@Valid CollectionLinkDeletionDto> links
    ) {
        return ResponseEntity.ok(collectionService.syncDeletedCollectionLinks(links, controllerUtil.getCurrentUser().getId()));
    }
}
