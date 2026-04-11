package com.emeraldgrove.service;

import com.emeraldgrove.dto.ArticleAiResponse;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.entity.User;

import java.util.List;

public interface ArticleService {
    SyncArticleResponse syncArticle(SyncArticleRequest request, User user);
    List<ArticleSyncDto> getAll(Long userId);
    void deleteNote(String articleExternalId, String noteExternalId, Long userId);
    void deleteArticle(String externalId, Long userId);
    ArticleAiResponse getAiResult(String externalId, Long userId);
}
