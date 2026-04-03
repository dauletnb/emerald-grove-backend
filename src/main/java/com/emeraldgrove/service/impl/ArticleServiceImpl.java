package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.ArticleNoteDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.dto.SyncArticleResponse;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleNote;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;

    @Override
    @Transactional
    public SyncArticleResponse sync(SyncArticleRequest request) {
        Optional<Article> byExternalId = Optional.ofNullable(request.externalId())
                .flatMap(articleRepository::findByExternalId);
        Optional<Article> byUrl = articleRepository.findByUrl(request.url());

        Article article = byExternalId.or(() -> byUrl).orElseGet(() -> Article.builder()
                .notes(new ArrayList<>())
                .build());

        boolean isNewArticle = article.getId() == null;

        if (article.getExternalId() == null && request.externalId() != null) {
            article.setExternalId(request.externalId());
        }

        article.setUrl(request.url());
        article.setTitle(request.title());
        article.setDescription(request.description());
        mergeNotes(article, request.notes());

        Article saved = articleRepository.save(article);

        return new SyncArticleResponse(
                isNewArticle ? SyncStatus.CREATED : SyncStatus.UPDATED,
                toDto(saved)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ArticleSyncDto> getAll() {
        return articleRepository.findAll().stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(ArticleSyncDto::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private void mergeNotes(Article article, List<ArticleNoteDto> incomingNotes) {
        Map<String, ArticleNote> existingByExternalId = new LinkedHashMap<>();

        for (ArticleNote note : article.getNotes()) {
            existingByExternalId.put(note.getExternalId(), note);
        }

        for (ArticleNoteDto incomingNote : incomingNotes == null ? List.<ArticleNoteDto>of() : incomingNotes) {
            ArticleNote note = existingByExternalId.get(incomingNote.id());

            if (note == null) {
                note = ArticleNote.builder()
                        .externalId(incomingNote.id())
                        .article(article)
                        .build();
                article.getNotes().add(note);
                existingByExternalId.put(incomingNote.id(), note);
            }

            note.setType(incomingNote.type());
            note.setContent(incomingNote.content().trim());
            note.setArticle(article);
        }
    }

    private ArticleSyncDto toDto(Article article) {
        List<ArticleNoteDto> notes = article.getNotes().stream()
                .sorted(Comparator.comparing(ArticleNote::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(note -> new ArticleNoteDto(
                        note.getExternalId(),
                        note.getType(),
                        note.getContent(),
                        note.getCreatedAt() == null ? null : note.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli()
                ))
                .toList();

        return new ArticleSyncDto(
                article.getId(),
                article.getExternalId(),
                article.getUrl(),
                article.getTitle(),
                article.getDescription(),
                article.getIsRead(),
                article.getCreatedAt() == null ? null : article.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli(),
                article.getUpdatedAt() == null ? null : article.getUpdatedAt().toInstant(ZoneOffset.UTC).toEpochMilli(),
                notes
        );
    }
}
