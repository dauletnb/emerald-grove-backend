package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.exception.DuplicateArticleException;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;

    @Override
    @Transactional
    public SyncArticleResponse sync(SyncArticleRequest request) {
        if (request.externalId() != null) {
            Optional<Article> byExternalId = articleRepository.findByExternalId(request.externalId());
            if (byExternalId.isPresent()) {
                throw new DuplicateArticleException(byExternalId.get().getId());
            }
        }

        Optional<Article> existing = articleRepository.findByUrl(request.url());
        if (existing.isPresent()) {
            throw new DuplicateArticleException(existing.get().getId());
        }

        try {
            Article article = Article.builder()
                .externalId(request.externalId())
                .url(request.url())
                .title(request.title())
                .description(request.description())
                .build();

            Article saved = articleRepository.save(article);
            return new SyncArticleResponse(SyncStatus.CREATED, saved.getId());
        } catch (DataIntegrityViolationException e) {
            Article raceExisting = articleRepository.findByUrl(request.url())
                .orElseThrow(() -> new IllegalStateException("Race condition: article not found after conflict"));
            throw new DuplicateArticleException(raceExisting.getId());
        }
    }
}