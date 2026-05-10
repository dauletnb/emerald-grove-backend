package com.emeraldgrove.service;

import com.emeraldgrove.dto.CollectionDto;
import com.emeraldgrove.dto.CollectionLinkSyncDto;
import com.emeraldgrove.dto.CollectionRequestDto;
import com.emeraldgrove.dto.CollectionSyncDto;

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

    void syncCollections(List<CollectionSyncDto> collections, Long userId);

    void syncCollectionLinks(List<CollectionLinkSyncDto> links, Long userId);
}
