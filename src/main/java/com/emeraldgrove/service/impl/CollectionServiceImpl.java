package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.CollectionDto;
import com.emeraldgrove.dto.CollectionLinkSyncDto;
import com.emeraldgrove.dto.CollectionRequestDto;
import com.emeraldgrove.dto.CollectionSyncDto;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleCollection;
import com.emeraldgrove.entity.ArticleCollectionLink;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.repository.ArticleCollectionLinkRepository;
import com.emeraldgrove.repository.ArticleCollectionRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.service.CollectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CollectionServiceImpl implements CollectionService {
    private final ArticleCollectionRepository collectionRepository;
    private final ArticleCollectionLinkRepository linkRepository;
    private final ArticleRepository articleRepository;

    @Override
    @Transactional
    public CollectionDto createCollection(CollectionRequestDto request, Long userId) {
        ArticleCollection collection = ArticleCollection.builder()
            .externalId(UUID.randomUUID().toString())
            .user(User.builder().id(userId).build())
            .name(request.name().trim())
            .build();

        ArticleCollection saved = collectionRepository.save(collection);
        return toDto(saved, List.of());
    }

    @Override
    @Transactional
    public CollectionDto renameCollection(String externalId, CollectionRequestDto request, Long userId) {
        ArticleCollection collection = getOwnedCollection(externalId, userId);
        collection.setName(request.name().trim());
        ArticleCollection saved = collectionRepository.save(collection);
        return toDto(saved, linkRepository.findArticleExternalIdsByCollectionExternalId(externalId));
    }

    @Override
    @Transactional
    public void deleteCollection(String externalId, Long userId) {
        collectionRepository.delete(getOwnedCollection(externalId, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionSyncDto> getAllCollections(Long userId) {
        return collectionRepository.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
            .map(this::toSyncDto)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CollectionDto getCollection(String externalId, Long userId) {
        ArticleCollection collection = getOwnedCollection(externalId, userId);
        return toDto(collection, linkRepository.findArticleExternalIdsByCollectionExternalId(externalId));
    }

    @Override
    @Transactional
    public void addArticleToCollection(String articleExternalId, String collectionExternalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(articleExternalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + articleExternalId));
        ArticleCollection collection = getOwnedCollection(collectionExternalId, userId);

        if (linkRepository.findByArticleExternalIdAndCollectionExternalId(articleExternalId, collectionExternalId).isPresent()) {
            return;
        }

        linkRepository.save(ArticleCollectionLink.builder()
            .externalId(UUID.randomUUID().toString())
            .article(article)
            .collection(collection)
            .build());
    }

    @Override
    @Transactional
    public void removeArticleFromCollection(String articleExternalId, String collectionExternalId, Long userId) {
        getOwnedCollection(collectionExternalId, userId);
        linkRepository.deleteByArticleExternalIdAndCollectionExternalId(articleExternalId, collectionExternalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCollectionArticleIds(String collectionExternalId, Long userId) {
        getOwnedCollection(collectionExternalId, userId);
        return linkRepository.findArticleExternalIdsByCollectionExternalId(collectionExternalId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getArticleCollectionIds(String articleExternalId, Long userId) {
        articleRepository.findByExternalIdAndUserId(articleExternalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + articleExternalId));
        return linkRepository.findCollectionExternalIdsByArticleExternalId(articleExternalId);
    }

    @Override
    @Transactional
    public void syncCollections(List<CollectionSyncDto> collections, Long userId) {
        for (CollectionSyncDto dto : collections) {
            ArticleCollection existing = collectionRepository.findByExternalIdAndUserId(dto.externalId(), userId)
                .orElse(null);

            if (existing != null) {
                String trimmedName = dto.name().trim();
                if (!existing.getName().equals(trimmedName)) {
                    existing.setName(trimmedName);
                    collectionRepository.save(existing);
                }
                continue;
            }

            collectionRepository.save(ArticleCollection.builder()
                .externalId(dto.externalId())
                .user(User.builder().id(userId).build())
                .name(dto.name().trim())
                .build());
        }
    }

    @Override
    @Transactional
    public void syncCollectionLinks(List<CollectionLinkSyncDto> links, Long userId) {
        for (CollectionLinkSyncDto dto : links) {
            Article article = articleRepository.findByExternalIdAndUserId(dto.articleExternalId(), userId).orElse(null);
            ArticleCollection collection = collectionRepository.findByExternalIdAndUserId(dto.collectionExternalId(), userId)
                .orElse(null);

            if (article == null || collection == null) {
                log.warn(
                    "Skipping collection link sync for user {}: article={}, collection={}",
                    userId,
                    dto.articleExternalId(),
                    dto.collectionExternalId()
                );
                continue;
            }

            if (linkRepository.findByArticleExternalIdAndCollectionExternalId(dto.articleExternalId(), dto.collectionExternalId()).isPresent()) {
                continue;
            }

            linkRepository.save(ArticleCollectionLink.builder()
                .externalId(dto.externalId())
                .article(article)
                .collection(collection)
                .clientCreatedAt(toLocalDateTime(dto.clientCreatedAt()))
                .build());
        }
    }

    private ArticleCollection getOwnedCollection(String externalId, Long userId) {
        return collectionRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Коллекция не найдена: " + externalId));
    }

    private CollectionSyncDto toSyncDto(ArticleCollection collection) {
        return new CollectionSyncDto(
            collection.getExternalId(),
            collection.getName()
        );
    }

    private CollectionDto toDto(ArticleCollection collection, List<String> articleIds) {
        return new CollectionDto(
            collection.getId(),
            collection.getExternalId(),
            collection.getName(),
            toEpochMillis(collection.getCreatedAt()),
            toEpochMillis(collection.getUpdatedAt()),
            articleIds.size(),
            articleIds
        );
    }

    private Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value).getTime();
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return epochMillis == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }
}
