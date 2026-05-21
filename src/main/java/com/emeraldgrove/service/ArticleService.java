package com.emeraldgrove.service;

import com.emeraldgrove.dto.article.ArticleDeletionSyncRequestDto;
import com.emeraldgrove.dto.article.ArticleSyncDto;
import com.emeraldgrove.dto.sync.SyncBatchResponseDto;
import com.emeraldgrove.dto.sync.SyncArticleRequestDto;
import com.emeraldgrove.dto.sync.SyncArticleResponseDto;
import com.emeraldgrove.entity.User;

import java.util.List;

public interface ArticleService {
    SyncArticleResponseDto syncArticle(SyncArticleRequestDto request, User user);

    SyncBatchResponseDto syncDeletedArticles(ArticleDeletionSyncRequestDto request, Long userId);

    List<ArticleSyncDto> getAll(Long userId);

    void deleteNote(String articleExternalId, String noteExternalId, Long userId);

    void deleteArticle(String externalId, Long userId);

    List<String> getArticleCollectionIds(String articleExternalId, Long userId);
}