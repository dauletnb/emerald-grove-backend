package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.ArticleNoteDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleNoteRequest;
import com.emeraldgrove.dto.SyncArticlePayloadResponse;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.security.XssSanitizer;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.service.ArticleSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    private final ArticleRepository articleRepository;
    private final XssSanitizer xssSanitizer;
    private final ArticleSummaryService articleSummaryService;

    @Override
    @Transactional
    public SyncArticleResponse syncArticle(SyncArticleRequest request) {
        Article existing = findExistingArticle(request);

        if (existing != null) {
            updateArticle(existing, request);
            syncNotes(existing, request.notes());

            Article saved = articleRepository.save(existing);
            return new SyncArticleResponse(SyncStatus.UPDATED, saved.getId(), SyncArticlePayloadResponse.fromEntity(saved));
        }

        try {
            String summaryDescription = articleSummaryService.summarizeDescription(
                request.title(),
                request.description(),
                null
            );

            Article article = Article.builder()
                .externalId(request.externalId())
                .url(request.url())
                .title(sanitizeText(request.title()))
                .description(sanitizeText(summaryDescription))
                .build();

            syncNotes(article, request.notes());

            Article saved = articleRepository.save(article);
            return new SyncArticleResponse(SyncStatus.CREATED, saved.getId(), SyncArticlePayloadResponse.fromEntity(saved));
        } catch (DataIntegrityViolationException e) {
            Article raceExisting = findExistingArticle(request);

            if (raceExisting == null) {
                throw new IllegalStateException("Race condition: article not found after conflict", e);
            }

            updateArticle(raceExisting, request);
            syncNotes(raceExisting, request.notes());

            Article saved = articleRepository.save(raceExisting);
            return new SyncArticleResponse(SyncStatus.UPDATED, saved.getId(), SyncArticlePayloadResponse.fromEntity(saved));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleSyncDto> getAll() {
        return articleRepository.findAll().stream()
            .map(this::toDto)
            .toList();
    }

    private Article findExistingArticle(SyncArticleRequest request) {
        if (request.externalId() != null && !request.externalId().isBlank()) {
            Article byExternalId = articleRepository.findByExternalId(request.externalId()).orElse(null);
            if (byExternalId != null) {
                return byExternalId;
            }
        }

        return articleRepository.findByUrl(request.url()).orElse(null);
    }

    private void updateArticle(Article article, SyncArticleRequest request) {
        if ((article.getExternalId() == null || article.getExternalId().isBlank())
            && request.externalId() != null
            && !request.externalId().isBlank()) {
            article.setExternalId(request.externalId());
        }

        String summaryDescription = articleSummaryService.summarizeDescription(
            request.title(),
            request.description(),
            article.getDescription()
        );

        article.setTitle(sanitizeText(request.title()));
        article.setUrl(request.url());
        article.setDescription(sanitizeText(summaryDescription));
    }

    private void syncNotes(Article article, List<SyncArticleNoteRequest> requestNotes) {
        List<SyncArticleNoteRequest> safeRequestNotes = requestNotes == null ? List.of() : requestNotes;

        Map<String, ArticleNote> existingByExternalId = new HashMap<>();
        for (ArticleNote note : article.getNotes()) {
            existingByExternalId.put(note.getExternalId(), note);
        }

        List<ArticleNote> nextNotes = new ArrayList<>();

        for (SyncArticleNoteRequest requestNote : safeRequestNotes) {
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
            note.setClientCreatedAt(requestNote.createdAt());

            nextNotes.add(note);
        }

        article.getNotes().clear();
        article.getNotes().addAll(nextNotes);
    }

    @Override
    @Transactional
    public void deleteArticle(String externalId) {
        Article article = articleRepository.findByExternalId(externalId)
            .orElseThrow(() -> new EntityNotFoundException("Article not found: " + externalId));
        articleRepository.delete(article);
    }

    @Override
    @Transactional
    public void deleteNote(String articleExternalId, String noteExternalId) {
        Article article = articleRepository.findByExternalId(articleExternalId)
            .orElseThrow(() -> new EntityNotFoundException("Article not found: " + articleExternalId));

        boolean removed = article.getNotes().removeIf(note -> note.getExternalId().equals(noteExternalId));

        if (!removed) {
            throw new EntityNotFoundException("Note not found: " + noteExternalId);
        }

        articleRepository.save(article);
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
            article.getIsRead(),
            toEpochMillis(article.getCreatedAt()),
            toEpochMillis(article.getUpdatedAt()),
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
            note.getClientCreatedAt()
        );
    }

    private Long toEpochMillis(java.time.LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value).getTime();
    }
}
