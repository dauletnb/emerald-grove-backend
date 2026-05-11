package com.emeraldgrove.service;

import com.emeraldgrove.dto.ArticleDeletionSyncRequestDto;
import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.repository.AiJobRepository;
import com.emeraldgrove.repository.AiResultRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.security.XssSanitizer;
import com.emeraldgrove.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private AiJobRepository aiJobRepository;

    @Mock
    private XssSanitizer xssSanitizer;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Test
    void syncArticleCreatesWithFavoriteAndReadLaterFlags() {
        User user = User.builder().id(1L).build();
        SyncArticleRequestDto request = new SyncArticleRequestDto(
            "article-1",
            "https://example.com/article",
            "Interesting article",
            "Short description",
            true,
            false,
            List.of()
        );

        when(xssSanitizer.sanitize("Interesting article")).thenReturn("Interesting article");
        when(xssSanitizer.sanitize("Short description")).thenReturn("Short description");
        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.empty());
        when(articleRepository.findByUrlAndUserId("https://example.com/article", 1L)).thenReturn(Optional.empty());
        when(articleRepository.save(any(Article.class))).thenAnswer(invocation -> {
            Article article = invocation.getArgument(0);
            article.setId(42L);
            return article;
        });
        when(aiJobRepository.findTopByArticleIdAndTypeOrderByCreatedAtDesc(42L, AiJob.TYPE_FULL_ANALYSIS))
            .thenReturn(Optional.empty());

        SyncArticleResponseDto response = articleService.syncArticle(request, user);

        ArgumentCaptor<Article> captor = ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(captor.capture());
        assertThat(captor.getValue().isFavorite()).isTrue();
        assertThat(captor.getValue().isReadLater()).isFalse();
        assertThat(response.status()).isEqualTo(SyncStatus.CREATED);
        assertThat(response.article().isFavorite()).isTrue();
        assertThat(response.article().isReadLater()).isFalse();
    }

    @Test
    void syncArticleUpdatesFavoriteAndReadLaterFlags() {
        User user = User.builder().id(1L).build();
        Article existing = Article.builder()
            .id(42L)
            .externalId("article-1")
            .user(user)
            .title("Old title")
            .url("https://example.com/article")
            .description("Old description")
            .isFavorite(false)
            .isReadLater(true)
            .build();

        SyncArticleRequestDto request = new SyncArticleRequestDto(
            "article-1",
            "https://example.com/article",
            "New title",
            "New description",
            true,
            false,
            List.of()
        );

        when(xssSanitizer.sanitize("New title")).thenReturn("New title");
        when(xssSanitizer.sanitize("New description")).thenReturn("New description");
        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.of(existing));
        when(articleRepository.save(existing)).thenReturn(existing);
        when(aiJobRepository.findTopByArticleIdAndTypeOrderByCreatedAtDesc(42L, AiJob.TYPE_FULL_ANALYSIS))
            .thenReturn(Optional.of(AiJob.createFullAnalysisJob(existing)));

        SyncArticleResponseDto response = articleService.syncArticle(request, user);

        assertThat(existing.isFavorite()).isTrue();
        assertThat(existing.isReadLater()).isFalse();
        assertThat(response.status()).isEqualTo(SyncStatus.UPDATED);
        assertThat(response.article().isFavorite()).isTrue();
        assertThat(response.article().isReadLater()).isFalse();
    }

    @Test
    void getAllReturnsFavoriteAndReadLaterFlags() {
        Article article = Article.builder()
            .id(42L)
            .externalId("article-1")
            .user(User.builder().id(1L).build())
            .title("Interesting article")
            .url("https://example.com/article")
            .description("Short description")
            .isFavorite(true)
            .isReadLater(false)
            .build();

        when(articleRepository.findAllByUserId(1L)).thenReturn(List.of(article));

        List<ArticleSyncDto> articles = articleService.getAll(1L);

        assertThat(articles).singleElement().satisfies(item -> {
            assertThat(item.isFavorite()).isTrue();
            assertThat(item.isReadLater()).isFalse();
        });
    }

    @Test
    void syncDeletedArticlesIsIdempotent() {
        Article article = Article.builder()
            .id(42L)
            .externalId("article-1")
            .user(User.builder().id(1L).build())
            .title("Interesting article")
            .url("https://example.com/article")
            .description("Short description")
            .build();

        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.of(article));
        when(articleRepository.findByExternalIdAndUserId("article-2", 1L)).thenReturn(Optional.empty());

        var result = articleService.syncDeletedArticles(new ArticleDeletionSyncRequestDto(List.of("article-1", "article-2")), 1L);

        verify(articleRepository).delete(article);
        assertThat(result.appliedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skipped()).singleElement().satisfies(item ->
            assertThat(item.reason()).isEqualTo("ARTICLE_NOT_FOUND")
        );
    }
}
