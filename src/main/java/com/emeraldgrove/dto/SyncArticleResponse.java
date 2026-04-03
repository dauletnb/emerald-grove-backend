// SyncArticleResponse.java
package com.emeraldgrove.dto;

import com.emeraldgrove.enums.SyncStatus;

public record SyncArticleResponse(SyncStatus status, ArticleSyncDto article) {}
