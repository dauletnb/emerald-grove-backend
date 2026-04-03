// ArticleService.java
package com.emeraldgrove.service;

import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;

import java.util.List;

public interface ArticleService {
    SyncArticleResponse sync(SyncArticleRequest request);
    List<ArticleSyncDto> getAll();
}
