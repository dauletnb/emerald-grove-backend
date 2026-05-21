package com.emeraldgrove.service;

import com.emeraldgrove.dto.collection.CollectionDto;
import com.emeraldgrove.dto.collection.CollectionLinkBatchSyncResponseDto;
import com.emeraldgrove.dto.collection.CollectionLinkDeletionDto;
import com.emeraldgrove.dto.collection.CollectionLinkSyncDto;
import com.emeraldgrove.dto.collection.CollectionRequestDto;
import com.emeraldgrove.dto.collection.CollectionSyncDto;
import com.emeraldgrove.dto.sync.SyncBatchResponseDto;

import java.util.List;

public interface CollectionService {
    CollectionDto createCollection(CollectionRequestDto request, Long userId);

    CollectionDto renameCollection(String externalId, CollectionRequestDto request, Long userId);

    void deleteCollection(String externalId, Long userId);

    List<CollectionSyncDto> getAllCollections(Long userId);

    CollectionDto getCollection(String externalId, Long userId);

    void addArticleToCollection(String articleExternalId, String collectionExternalId, Long userId);

    void removeArticleFromCollection(String articleExternalId, String collectionExternalId, Long userId);

    List<String> getCollectionArticleIds(String collectionExternalId, Long userId);

    List<String> getArticleCollectionIds(String articleExternalId, Long userId);

    SyncBatchResponseDto syncCollections(List<CollectionSyncDto> collections, Long userId);

    CollectionLinkBatchSyncResponseDto syncCollectionLinks(List<CollectionLinkSyncDto> links, Long userId);

    SyncBatchResponseDto syncDeletedCollections(List<String> externalIds, Long userId);

    CollectionLinkBatchSyncResponseDto syncDeletedCollectionLinks(List<CollectionLinkDeletionDto> links, Long userId);
}
