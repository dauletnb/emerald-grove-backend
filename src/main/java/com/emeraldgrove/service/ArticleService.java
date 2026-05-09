package com.emeraldgrove.service;

import com.emeraldgrove.dto.ArticleAiResponseDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.entity.User;

import java.util.List;

public interface ArticleService {
    SyncArticleResponseDto syncArticle(SyncArticleRequestDto request, User user);
    List<ArticleSyncDto> getAll(Long userId);
    void deleteNote(String articleExternalId, String noteExternalId, Long userId);
    void deleteArticle(String externalId, Long userId);
    ArticleAiResponseDto getAiResult(String externalId, Long userId);
    void retryAiAnalysis(String externalId, Long userId);
}
