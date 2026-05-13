package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleDeletionSyncRequestDto;
import com.emeraldgrove.dto.CollectionLinkBatchSyncResponseDto;
import com.emeraldgrove.dto.CollectionLinkDeletionDto;
import com.emeraldgrove.dto.CollectionLinkSyncDto;
import com.emeraldgrove.dto.CollectionLinkSyncResultDto;
import com.emeraldgrove.dto.ExternalIdDeletionRequestDto;
import com.emeraldgrove.dto.SyncArticlePayloadResponseDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
import com.emeraldgrove.dto.SyncBatchItemResultDto;
import com.emeraldgrove.dto.SyncBatchResponseDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.exception.GlobalExceptionHandler;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.service.CollectionService;
import com.emeraldgrove.util.ControllerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SyncControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private CollectionService collectionService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new SyncController(articleService, collectionService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        lenient().when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
    }

    @Test
    void syncArticleReturnsFlagsInPayload() throws Exception {
        SyncArticleRequestDto request = new SyncArticleRequestDto(
            "article-1",
            "https://example.com/article",
            "Interesting article",
            "Short description",
            null,   // content
            true,
            false,
            List.of()
        );

        when(articleService.syncArticle(any(SyncArticleRequestDto.class), any(User.class)))
            .thenReturn(new SyncArticleResponseDto(
                SyncStatus.CREATED,
                42L,
                new SyncArticlePayloadResponseDto(
                    42L,
                    "article-1",
                    "https://example.com/article",
                    "Interesting article",
                    "Short description",
                    null,   // content
                    true,
                    false,
                    1712160000000L,
                    1712160005000L,
                    "PENDING",
                    List.of()
                )
            ));

        mockMvc.perform(post("/api/sync/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.article.isFavorite").value(true))
            .andExpect(jsonPath("$.article.isReadLater").value(false));
    }

    @Test
    void syncArticleWithContentReturnsContentInPayload() throws Exception {
        String htmlContent = "<html><body><h1>Full article HTML</h1><p>Paragraph text.</p></body></html>";

        SyncArticleRequestDto request = new SyncArticleRequestDto(
            "test-uuid-001",
            "https://example.com/article-with-content",
            "Test Article With Content",
            "Short description",
            htmlContent,
            false,
            false,
            List.of()
        );

        when(articleService.syncArticle(any(SyncArticleRequestDto.class), any(User.class)))
            .thenReturn(new SyncArticleResponseDto(
                SyncStatus.CREATED,
                1L,
                new SyncArticlePayloadResponseDto(
                    1L,
                    "test-uuid-001",
                    "https://example.com/article-with-content",
                    "Test Article With Content",
                    "Short description",
                    htmlContent,
                    false,
                    false,
                    1712160000000L,
                    1712160005000L,
                    "PENDING",
                    List.of()
                )
            ));

        mockMvc.perform(post("/api/sync/articles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andExpect(jsonPath("$.articleId").value(1))
            .andExpect(jsonPath("$.article.externalId").value("test-uuid-001"))
            .andExpect(jsonPath("$.article.title").value("Test Article With Content"))
            .andExpect(jsonPath("$.article.content").value(htmlContent))
            .andExpect(jsonPath("$.article.isFavorite").value(false))
            .andExpect(jsonPath("$.article.isReadLater").value(false));
    }

    @Test
    void syncDeletedArticlesReturnsDiagnostics() throws Exception {
        ArticleDeletionSyncRequestDto request = new ArticleDeletionSyncRequestDto(List.of("article-1", "article-2"));

        when(articleService.syncDeletedArticles(eq(request), eq(1L)))
            .thenReturn(new SyncBatchResponseDto(
                1,
                1,
                List.of(new SyncBatchItemResultDto("article-1", "APPLIED", null)),
                List.of(new SyncBatchItemResultDto("article-2", "SKIPPED", "ARTICLE_NOT_FOUND"))
            ));

        mockMvc.perform(post("/api/sync/articles/deletions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appliedCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.skipped[0].reason").value("ARTICLE_NOT_FOUND"));
    }

    @Test
    void syncCollectionLinksReturnsDiagnostics() throws Exception {
        when(collectionService.syncCollectionLinks(eq(List.of(
            new CollectionLinkSyncDto("link-1", "article-1", "collection-1", 1712160000000L)
        )), eq(1L))).thenReturn(new CollectionLinkBatchSyncResponseDto(
            0,
            1,
            List.of(),
            List.of(new CollectionLinkSyncResultDto("link-1", "article-1", "collection-1", "SKIPPED", "ARTICLE_NOT_FOUND"))
        ));

        mockMvc.perform(post("/api/sync/collections/links")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                List.of(new CollectionLinkSyncDto(
                    "link-1",
                    "article-1",
                    "collection-1",
                    1712160000000L
                ))
            )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.skipped[0].reason").value("ARTICLE_NOT_FOUND"));
    }

    @Test
    void syncDeletedCollectionsReturnsDiagnostics() throws Exception {
        when(collectionService.syncDeletedCollections(eq(List.of("collection-1")), eq(1L)))
            .thenReturn(new SyncBatchResponseDto(
                1,
                0,
                List.of(new SyncBatchItemResultDto("collection-1", "APPLIED", null)),
                List.of()
            ));

        mockMvc.perform(post("/api/sync/collections/deletions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ExternalIdDeletionRequestDto(List.of("collection-1")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appliedCount").value(1));
    }

    @Test
    void syncDeletedCollectionLinksReturnsDiagnostics() throws Exception {
        when(collectionService.syncDeletedCollectionLinks(eq(List.of(
            new CollectionLinkDeletionDto("link-1", "article-1", "collection-1")
        )), eq(1L))).thenReturn(new CollectionLinkBatchSyncResponseDto(
            1,
            0,
            List.of(new CollectionLinkSyncResultDto("link-1", "article-1", "collection-1", "APPLIED", null)),
            List.of()
        ));

        mockMvc.perform(post("/api/sync/collections/links/deletions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(
                    new CollectionLinkDeletionDto("link-1", "article-1", "collection-1")
                ))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appliedCount").value(1));
    }
}
