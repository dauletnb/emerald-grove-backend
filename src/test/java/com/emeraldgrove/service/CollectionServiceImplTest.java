package com.emeraldgrove.service;

import com.emeraldgrove.dto.CollectionLinkDeletionDto;
import com.emeraldgrove.dto.CollectionLinkSyncDto;
import com.emeraldgrove.dto.CollectionRequestDto;
import com.emeraldgrove.dto.CollectionSyncDto;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.entity.ArticleCollection;
import com.emeraldgrove.entity.ArticleCollectionLink;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.repository.ArticleCollectionLinkRepository;
import com.emeraldgrove.repository.ArticleCollectionRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.emeraldgrove.service.impl.CollectionServiceImpl;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionServiceImplTest {

    @Mock
    private ArticleCollectionRepository collectionRepository;

    @Mock
    private ArticleCollectionLinkRepository linkRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private CollectionServiceImpl collectionService;

    @Test
    void createCollectionGeneratesAndTrimsName() {
        ArticleCollection persisted = ArticleCollection.builder()
            .id(10L)
            .externalId("generated-id")
            .user(User.builder().id(1L).build())
            .name("My collection")
            .build();

        when(collectionRepository.save(any(ArticleCollection.class))).thenReturn(persisted);

        var result = collectionService.createCollection(new CollectionRequestDto("  My collection  "), 1L);

        ArgumentCaptor<ArticleCollection> captor = ArgumentCaptor.forClass(ArticleCollection.class);
        verify(collectionRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("My collection");
        assertThat(captor.getValue().getExternalId()).isNotBlank();
        assertThat(result.name()).isEqualTo("My collection");
    }

    @Test
    void addArticleToCollectionIsIdempotentWhenLinkExists() {
        Article article = Article.builder()
            .id(5L)
            .externalId("article-1")
            .user(User.builder().id(1L).build())
            .title("Title")
            .url("https://example.com")
            .build();
        ArticleCollection collection = ArticleCollection.builder()
            .id(8L)
            .externalId("collection-1")
            .user(User.builder().id(1L).build())
            .name("Collection")
            .build();

        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.of(article));
        when(collectionRepository.findByExternalIdAndUserId("collection-1", 1L)).thenReturn(Optional.of(collection));
        when(linkRepository.findByArticleExternalIdAndCollectionExternalId("article-1", "collection-1"))
            .thenReturn(Optional.of(ArticleCollectionLink.builder().externalId("link-1").build()));

        collectionService.addArticleToCollection("article-1", "collection-1", 1L);

        verify(linkRepository, never()).save(any(ArticleCollectionLink.class));
    }

    @Test
    void syncCollectionsCreatesMissingCollection() {
        when(collectionRepository.findByExternalIdAndUserId("collection-1", 1L)).thenReturn(Optional.empty());

        var result = collectionService.syncCollections(List.of(new CollectionSyncDto("collection-1", "Saved")), 1L);

        ArgumentCaptor<ArticleCollection> captor = ArgumentCaptor.forClass(ArticleCollection.class);
        verify(collectionRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("collection-1");
        assertThat(captor.getValue().getName()).isEqualTo("Saved");
        assertThat(captor.getValue().getUser().getId()).isEqualTo(1L);
        assertThat(result.appliedCount()).isEqualTo(1);
    }

    @Test
    void syncCollectionsUpdatesNameForExistingCollection() {
        ArticleCollection existing = ArticleCollection.builder()
            .id(7L)
            .externalId("collection-1")
            .user(User.builder().id(1L).build())
            .name("Old")
            .build();

        when(collectionRepository.findByExternalIdAndUserId("collection-1", 1L)).thenReturn(Optional.of(existing));

        var result = collectionService.syncCollections(List.of(new CollectionSyncDto("collection-1", "New")), 1L);

        verify(collectionRepository).save(existing);
        assertThat(existing.getName()).isEqualTo("New");
        assertThat(result.appliedCount()).isEqualTo(1);
    }

    @Test
    void syncCollectionLinksSkipsMissingArticleOrCollection() {
        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.empty());

        var result = collectionService.syncCollectionLinks(
            List.of(new CollectionLinkSyncDto("link-1", "article-1", "collection-1", 1712160000000L)),
            1L
        );

        verify(linkRepository, never()).save(any(ArticleCollectionLink.class));
        assertThat(result.skippedCount()).isEqualTo(1);
        assertThat(result.skipped()).singleElement().satisfies(item ->
            assertThat(item.reason()).isEqualTo("ARTICLE_NOT_FOUND")
        );
    }

    @Test
    void syncCollectionLinksCreatesNewLinkWhenDataExists() {
        Article article = Article.builder()
            .id(5L)
            .externalId("article-1")
            .user(User.builder().id(1L).build())
            .title("Title")
            .url("https://example.com")
            .build();
        ArticleCollection collection = ArticleCollection.builder()
            .id(8L)
            .externalId("collection-1")
            .user(User.builder().id(1L).build())
            .name("Collection")
            .build();

        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.of(article));
        when(collectionRepository.findByExternalIdAndUserId("collection-1", 1L)).thenReturn(Optional.of(collection));
        when(linkRepository.findByArticleExternalIdAndCollectionExternalId("article-1", "collection-1"))
            .thenReturn(Optional.empty());

        var result = collectionService.syncCollectionLinks(
            List.of(new CollectionLinkSyncDto("link-1", "article-1", "collection-1", 1712160000000L)),
            1L
        );

        ArgumentCaptor<ArticleCollectionLink> captor = ArgumentCaptor.forClass(ArticleCollectionLink.class);
        verify(linkRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalId()).isEqualTo("link-1");
        assertThat(captor.getValue().getArticle()).isEqualTo(article);
        assertThat(captor.getValue().getCollection()).isEqualTo(collection);
        assertThat(captor.getValue().getClientCreatedAt()).isNotNull();
        assertThat(result.appliedCount()).isEqualTo(1);
    }

    @Test
    void syncDeletedCollectionsIsIdempotent() {
        ArticleCollection collection = ArticleCollection.builder()
            .id(8L)
            .externalId("collection-1")
            .user(User.builder().id(1L).build())
            .name("Collection")
            .build();

        when(collectionRepository.findByExternalIdAndUserId("collection-1", 1L)).thenReturn(Optional.of(collection));
        when(collectionRepository.findByExternalIdAndUserId("collection-2", 1L)).thenReturn(Optional.empty());

        var result = collectionService.syncDeletedCollections(List.of("collection-1", "collection-2"), 1L);

        verify(collectionRepository).delete(collection);
        assertThat(result.appliedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    @Test
    void syncDeletedCollectionLinksIsIdempotent() {
        User user = User.builder().id(1L).build();
        Article article = Article.builder()
            .id(5L)
            .externalId("article-1")
            .user(user)
            .title("Title")
            .url("https://example.com")
            .build();
        ArticleCollection collection = ArticleCollection.builder()
            .id(8L)
            .externalId("collection-1")
            .user(user)
            .name("Collection")
            .build();
        ArticleCollectionLink link = ArticleCollectionLink.builder()
            .id(13L)
            .externalId("link-1")
            .article(article)
            .collection(collection)
            .build();

        when(linkRepository.findByArticleExternalIdAndCollectionExternalId("article-1", "collection-1"))
            .thenReturn(Optional.of(link));
        when(linkRepository.findByArticleExternalIdAndCollectionExternalId("article-2", "collection-2"))
            .thenReturn(Optional.empty());

        var result = collectionService.syncDeletedCollectionLinks(List.of(
            new CollectionLinkDeletionDto("link-1", "article-1", "collection-1"),
            new CollectionLinkDeletionDto("link-2", "article-2", "collection-2")
        ), 1L);

        verify(linkRepository).delete(link);
        assertThat(result.appliedCount()).isEqualTo(1);
        assertThat(result.skippedCount()).isEqualTo(1);
    }

    @Test
    void getArticleCollectionIdsChecksArticleOwnershipBeforeQuery() {
        Article article = Article.builder()
            .id(5L)
            .externalId("article-1")
            .user(User.builder().id(1L).build())
            .title("Title")
            .url("https://example.com")
            .build();
        when(articleRepository.findByExternalIdAndUserId("article-1", 1L)).thenReturn(Optional.of(article));
        when(linkRepository.findCollectionExternalIdsByArticleExternalId("article-1"))
            .thenReturn(List.of("collection-1", "collection-2"));

        List<String> result = collectionService.getArticleCollectionIds("article-1", 1L);

        verify(articleRepository).findByExternalIdAndUserId(eq("article-1"), eq(1L));
        assertThat(result).containsExactly("collection-1", "collection-2");
    }
}
