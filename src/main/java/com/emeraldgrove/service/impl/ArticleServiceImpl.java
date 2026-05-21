package com.emeraldgrove.service.impl;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emeraldgrove.constants.ErrorMessages;
import com.emeraldgrove.constants.SyncConstants;
import com.emeraldgrove.dto.article.ArticleDeletionSyncRequestDto;
import com.emeraldgrove.dto.article.ArticleNoteDto;
import com.emeraldgrove.dto.article.ArticleSyncDto;
import com.emeraldgrove.dto.sync.SyncArticleNoteRequestDto;
import com.emeraldgrove.dto.sync.SyncArticlePayloadResponseDto;
import com.emeraldgrove.dto.sync.SyncArticleRequestDto;
import com.emeraldgrove.dto.sync.SyncArticleResponseDto;
import com.emeraldgrove.dto.sync.SyncBatchItemResultDto;
import com.emeraldgrove.dto.sync.SyncBatchResponseDto;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.repository.ArticleNoteRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.security.XssSanitizer;
import com.emeraldgrove.service.AiService;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.service.CollectionService;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleNoteRepository articleNoteRepository;
    private final XssSanitizer xssSanitizer;
    private final CollectionService collectionService;
    private final AiService aiService;

    @Override
    @Transactional
    public SyncArticleResponseDto syncArticle(SyncArticleRequestDto request, User user) {
        Article existing = findExistingArticle(request, user.getId());

        if (existing != null) {
            updateArticle(existing, request);
            syncNotes(existing, request.notes());

            Article saved = articleRepository.save(existing);
            aiService.queueAnalysisIfAbsent(saved);
            return new SyncArticleResponseDto(SyncStatus.UPDATED, saved.getId(), SyncArticlePayloadResponseDto.fromEntity(saved));
        }

        try {
            Article article = Article.builder()
                .user(user)
                .externalId(request.externalId())
                .url(request.url())
                .title(sanitizeText(request.title()))
                .description(sanitizeText(request.description()))
                .content(request.content())
                .isFavorite(request.isFavorite())
                .isReadLater(request.isReadLater())
                .build();

            syncNotes(article, request.notes());

            Article saved = articleRepository.save(article);
            aiService.queueAnalysisIfAbsent(saved);
            return new SyncArticleResponseDto(SyncStatus.CREATED, saved.getId(), SyncArticlePayloadResponseDto.fromEntity(saved));
        } catch (DataIntegrityViolationException e) {
            Article raceExisting = findExistingArticle(request, user.getId());

            if (raceExisting == null) {
                throw new IllegalStateException(ErrorMessages.ERROR_RACE_CONDITION, e);
            }

            updateArticle(raceExisting, request);
            syncNotes(raceExisting, request.notes());

            Article saved = articleRepository.save(raceExisting);
            aiService.queueAnalysisIfAbsent(saved);
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
        log.info("Deleting article. externalId={}, userId={}", externalId, userId);
        Article article = articleRepository.findByExternalIdAndUserId(externalId, userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.ERROR_ARTICLE_NOT_FOUND.formatted(externalId)));
        articleRepository.delete(article);
        log.info("Deleted article. externalId={}, userId={}", externalId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getArticleCollectionIds(String articleExternalId, Long userId) {
        return collectionService.getArticleCollectionIds(articleExternalId, userId);
    }

    @Override
    @Transactional
    public SyncBatchResponseDto syncDeletedArticles(ArticleDeletionSyncRequestDto request, Long userId) {
        log.info("Syncing deleted articles. Count={}, userId={}", request.articleExternalIds().size(), userId);
        List<SyncBatchItemResultDto> applied = new ArrayList<>();
        List<SyncBatchItemResultDto> skipped = new ArrayList<>();

        for (String externalId : request.articleExternalIds()) {
            Article article = articleRepository.findByExternalIdAndUserId(externalId, userId).orElse(null);

            if (article == null) {
                log.debug("Skipping deleted article sync: article not found. externalId={}", externalId);
                skipped.add(new SyncBatchItemResultDto(externalId, SyncConstants.STATUS_SKIPPED, SyncConstants.ERROR_CODE_ARTICLE_NOT_FOUND));
                continue;
            }

            articleRepository.delete(article);
            log.debug("Deleted article during sync. externalId={}", externalId);
            applied.add(new SyncBatchItemResultDto(externalId, SyncConstants.STATUS_APPLIED, null));
        }

        log.info("Finished syncing deleted articles. applied={}, skipped={}", applied.size(), skipped.size());
        return new SyncBatchResponseDto(applied.size(), skipped.size(), applied, skipped);
    }

    @Override
    @Transactional
    public void deleteNote(String articleExternalId, String noteExternalId, Long userId) {
        log.info("Deleting note. noteExternalId={}, articleExternalId={}, userId={}", noteExternalId, articleExternalId, userId);
        articleRepository.findByExternalIdAndUserId(articleExternalId, userId)
            .orElseThrow(() -> new EntityNotFoundException(ErrorMessages.ERROR_NOTE_NOT_FOUND.formatted(articleExternalId)));

        articleNoteRepository.deleteByExternalIdAndArticleExternalId(noteExternalId, articleExternalId);
        log.info("Deleted note. noteExternalId={}, articleExternalId={}", noteExternalId, articleExternalId);
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
        article.setContent(request.content());
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
            article.getContent(),
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

    private Long toEpochMillis(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value).getTime();
    }

    private LocalDateTime toLocalDateTime(Long epochMillis) {
        return epochMillis == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
    }
}