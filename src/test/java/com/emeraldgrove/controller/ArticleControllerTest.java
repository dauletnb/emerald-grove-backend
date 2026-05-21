package com.emeraldgrove.controller;

import com.emeraldgrove.dto.article.ArticleDeletionSyncRequestDto;
import com.emeraldgrove.dto.article.ArticleSyncDto;
import com.emeraldgrove.dto.sync.SyncArticlePayloadResponseDto;
import com.emeraldgrove.dto.sync.SyncArticleRequestDto;
import com.emeraldgrove.dto.sync.SyncArticleResponseDto;
import com.emeraldgrove.dto.sync.SyncBatchItemResultDto;
import com.emeraldgrove.dto.sync.SyncBatchResponseDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.SyncStatus;
import com.emeraldgrove.exception.GlobalExceptionHandler;
import com.emeraldgrove.service.ArticleService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ArticleController(articleService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        lenient().when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
    }

    @Test
    void getArticlesReturnsFlags() throws Exception {
        when(articleService.getAll(eq(1L)))
            .thenReturn(List.of(new ArticleSyncDto(
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
            )));

        mockMvc.perform(get("/api/articles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].isFavorite").value(true))
            .andExpect(jsonPath("$[0].isReadLater").value(false));
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

        mockMvc.perform(post("/api/articles/sync")
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

        mockMvc.perform(post("/api/articles/sync")
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

        mockMvc.perform(post("/api/articles/sync/deletions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appliedCount").value(1))
            .andExpect(jsonPath("$.skippedCount").value(1))
            .andExpect(jsonPath("$.skipped[0].reason").value("ARTICLE_NOT_FOUND"));
    }
}
