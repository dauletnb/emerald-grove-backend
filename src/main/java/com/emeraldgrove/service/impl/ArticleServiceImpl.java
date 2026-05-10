package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.ArticleAiResponseDto;
import com.emeraldgrove.dto.ArticleNoteDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleNoteRequestDto;
import com.emeraldgrove.dto.SyncArticlePayloadResponseDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.entity.AiResult;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.repository.AiJobRepository;
import com.emeraldgrove.repository.AiResultRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.security.XssSanitizer;
import com.emeraldgrove.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final AiJobRepository aiJobRepository;
    private final AiResultRepository aiResultRepository;
    private final XssSanitizer xssSanitizer;

    @Override
    @Transactional
    public SyncArticleResponseDto syncArticle(SyncArticleRequestDto request, User user) {
        Article existing = findExistingArticle(request, user.getId());

        if (existing != null) {
            updateArticle(existing, request);
            syncNotes(existing, request.notes());

            Article saved = articleRepository.save(existing);
            createFullAnalysisJobIfAbsent(saved);
            return new SyncArticleResponseDto(SyncStatus.UPDATED, saved.getId(), SyncArticlePayloadResponseDto.fromEntity(saved));
        }

        try {
            Article article = Article.builder()
                .user(user)
                .externalId(request.externalId())
                .url(request.url())
                .title(sanitizeText(request.title()))
                .description(sanitizeText(request.description()))
                .isFavorite(request.isFavorite())
                .isReadLater(request.isReadLater())
                .build();

            syncNotes(article, request.notes());

            Article saved = articleRepository.save(article);
            createFullAnalysisJobIfAbsent(saved);
            return new SyncArticleResponseDto(SyncStatus.CREATED, saved.getId(), SyncArticlePayloadResponseDto.fromEntity(saved));
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request from the same user created the same article
            Article raceExisting = findExistingArticle(request, user.getId());

            if (raceExisting == null) {
                throw new IllegalStateException("Race condition: article not found after conflict", e);
            }

            updateArticle(raceExisting, request);
            syncNotes(raceExisting, request.notes());

            Article saved = articleRepository.save(raceExisting);
            createFullAnalysisJobIfAbsent(saved);
            return new SyncArticleResponseDto(SyncStatus.UPDATED, saved.getId(), SyncArticlePayloadResponseDto.fromEntity(saved));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleSyncDto> getAll(Long userId) {
        return articleRepository.findAllByUserId(userId).stream()
            .map(this::toDto)
            .toList();
    }

    @Override
    @Transactional
    public void deleteArticle(String externalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + externalId));
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public void deleteNote(String articleExternalId, String noteExternalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(articleExternalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Заметка не найдена: " + articleExternalId));

        boolean removed = article.getNotes().removeIf(note -> note.getExternalId().equals(noteExternalId));

        if (!removed) {
            throw new EntityNotFoundException("Заметка не найдена: " + noteExternalId);
        }

        articleRepository.save(article);
    }

    @Override
    @Transactional(readOnly = true)
    public ArticleAiResponseDto getAiResult(String externalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + externalId));

        String aiStatus = article.getAiStatus();
        AiResult aiResult = aiResultRepository
            .findTopByArticleIdAndTypeOrderByCreatedAtDesc(article.getId(), AiJob.TYPE_FULL_ANALYSIS)
            .orElse(null);

        String content = aiResult != null ? aiResult.getContent() : null;
        return new ArticleAiResponseDto(aiStatus, content);
    }

    @Override
    @Transactional
    public void retryAiAnalysis(String externalId, Long userId) {
        Article article = articleRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException("Статья не найдена: " + externalId));

        ensureFullAnalysisQueued(article);
    }

    private Article findExistingArticle(SyncArticleRequestDto request, Long userId) {
        if (request.externalId() != null && !request.externalId().isBlank()) {
            Article byExternalId = articleRepository.findByExternalIdAndUserId(request.externalId(), userId).orElse(null);
            if (byExternalId != null) {
                return byExternalId;
            }
        }
        return articleRepository.findByUrlAndUserId(request.url(), userId).orElse(null);
    }

    private void updateArticle(Article article, SyncArticleRequestDto request) {
        if ((article.getExternalId() == null || article.getExternalId().isBlank())
            && request.externalId() != null
            && !request.externalId().isBlank()) {
            article.setExternalId(request.externalId());
        }

        article.setTitle(sanitizeText(request.title()));
        article.setUrl(request.url());
        article.setDescription(sanitizeText(request.description()));
        article.setFavorite(request.isFavorite());
        article.setReadLater(request.isReadLater());
    }

    private void syncNotes(Article article, List<SyncArticleNoteRequestDto> requestNotes) {
        List<SyncArticleNoteRequestDto> safeRequestNotes = requestNotes == null ? List.of() : requestNotes;

        Map<String, ArticleNote> existingByExternalId = new HashMap<>();
        for (ArticleNote note : article.getNotes()) {
            existingByExternalId.put(note.getExternalId(), note);
        }

        List<ArticleNote> nextNotes = new ArrayList<>();

        for (SyncArticleNoteRequestDto requestNote : safeRequestNotes) {
            ArticleNote note = existingByExternalId.get(requestNote.id());

            if (note == null) {
                note = ArticleNote.builder()
                    .externalId(requestNote.id())
                    .article(article)
                    .build();
            }

            note.setArticle(article);
            note.setType(requestNote.type());
            note.setContent(sanitizeText(requestNote.content()));
            note.setClientCreatedAt(toLocalDateTime(requestNote.createdAt()));

            nextNotes.add(note);
        }

        article.getNotes().clear();
        article.getNotes().addAll(nextNotes);
    }

    private void createFullAnalysisJobIfAbsent(Article article) {
        ensureFullAnalysisQueued(article);
    }

    private void ensureFullAnalysisQueued(Article article) {
        AiJob latestJob = aiJobRepository
            .findTopByArticleIdAndTypeOrderByCreatedAtDesc(article.getId(), AiJob.TYPE_FULL_ANALYSIS)
            .orElse(null);

        if (latestJob == null) {
            article.setAiStatus("PENDING");
            aiJobRepository.save(AiJob.createFullAnalysisJob(article));
            return;
        }

        if ("DONE".equals(latestJob.getStatus()) || "PENDING".equals(latestJob.getStatus()) || "PROCESSING".equals(latestJob.getStatus())) {
            return;
        }

        latestJob.setStatus("PENDING");
        latestJob.setRetries(0);
        article.setAiStatus("PENDING");
        aiJobRepository.save(latestJob);
    }

    private String sanitizeText(String value) {
        return xssSanitizer.sanitize(value);
    }

    private ArticleSyncDto toDto(Article article) {
        return new ArticleSyncDto(
            article.getId(),
            article.getExternalId(),
            article.getUrl(),
            article.getTitle(),
            article.getDescription(),
            article.isFavorite(),
            article.isReadLater(),
            toEpochMillis(article.getCreatedAt()),
            toEpochMillis(article.getUpdatedAt()),
            article.getAiStatus(),
            article.getNotes().stream()
                .map(this::toDto)
                .toList()
        );
    }

    private ArticleNoteDto toDto(ArticleNote note) {
        return new ArticleNoteDto(
            note.getExternalId(),
            note.getType(),
            note.getContent(),
            toEpochMillis(note.getClientCreatedAt())
        );
    }

    private Long toEpochMillis(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value).getTime();
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return epochMillis == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }
}
