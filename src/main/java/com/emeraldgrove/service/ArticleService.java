package com.emeraldgrove.service;

import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;

public interface ArticleService {
    SyncArticleResponse syncArticle(SyncArticleRequest request);
}