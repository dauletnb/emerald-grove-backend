package com.emeraldgrove.service;

import com.emeraldgrove.dto.ai.AiAnalysisResultResponseDton;
import com.emeraldgrove.entity.Article;

public interface AiService {
    AiAnalysisResultResponseDton getAiResult(String externalId, Long userId);

    void retryAiAnalysis(String externalId, Long userId);

    void queueAnalysisIfAbsent(Article article);
}
