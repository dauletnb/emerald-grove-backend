package com.emeraldgrove.service.impl;

import com.emeraldgrove.constants.AiStatusConstants;
import com.emeraldgrove.constants.ErrorMessages;
import com.emeraldgrove.dto.ai.AiAnalysisResultResponseDton;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.entity.AiResult;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.repository.AiJobRepository;
import com.emeraldgrove.repository.AiResultRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.service.AiService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiServiceImpl implements AiService {

    private final ArticleRepository articleRepository;
    private final AiJobRepository aiJobRepository;
    private final AiResultRepository aiResultRepository;

    @Override
    @Transactional(readOnly = true)
    public AiAnalysisResultResponseDton getAiResult(String externalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.ERROR_ARTICLE_NOT_FOUND.formatted(externalId)));

        String aiStatus = article.getAiStatus();
        AiResult aiResult = aiResultRepository
            .findTopByArticleIdAndTypeOrderByCreatedAtDesc(article.getId(), AiJob.TYPE_FULL_ANALYSIS)
            .orElse(null);

        String content = aiResult != null ? aiResult.getContent() : null;
        return new AiAnalysisResultResponseDton(aiStatus, content);
    }

    @Override
    @Transactional
    public void retryAiAnalysis(String externalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.ERROR_ARTICLE_NOT_FOUND.formatted(externalId)));

        queueAnalysisIfAbsent(article);
    }

    @Override
    @Transactional
    public void queueAnalysisIfAbsent(Article article) {
        AiJob latestJob = aiJobRepository
            .findTopByArticleIdAndTypeOrderByCreatedAtDesc(article.getId(), AiJob.TYPE_FULL_ANALYSIS)
            .orElse(null);

        if (latestJob == null) {
            article.setAiStatus(AiStatusConstants.AI_STATUS_PENDING);
            aiJobRepository.save(AiJob.createFullAnalysisJob(article));
            log.debug("Queued new AI job for article id={}", article.getId());
            return;
        }

        if (AiStatusConstants.AI_STATUS_DONE.equals(latestJob.getStatus())
                || AiStatusConstants.AI_STATUS_PENDING.equals(latestJob.getStatus())
                || AiStatusConstants.AI_STATUS_PROCESSING.equals(latestJob.getStatus())) {
            return;
        }

        latestJob.setStatus(AiStatusConstants.AI_STATUS_PENDING);
        latestJob.setRetries(0);
        article.setAiStatus(AiStatusConstants.AI_STATUS_PENDING);
        aiJobRepository.save(latestJob);
        log.debug("Re-queued AI job for article id={}", article.getId());
    }
}
