package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ArticleSyncDto;
import com.emeraldgrove.dto.SyncArticlePayloadResponseDto;
import com.emeraldgrove.dto.SyncArticleRequestDto;
import com.emeraldgrove.dto.SyncArticleResponseDto;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerTest {

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
            .standaloneSetup(new ArticleController(articleService, collectionService, controllerUtil))
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
    void getArticlesReturnsFlags() throws Exception {
        when(articleService.getAll(eq(1L)))
            .thenReturn(List.of(new ArticleSyncDto(
                42L,
                "article-1",
                "https://example.com/article",
                "Interesting article",
                "Short description",
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
}
