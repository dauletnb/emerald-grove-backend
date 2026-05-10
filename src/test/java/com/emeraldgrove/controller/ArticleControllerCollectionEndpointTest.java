package com.emeraldgrove.controller;

import com.emeraldgrove.entity.User;
import com.emeraldgrove.exception.GlobalExceptionHandler;
import com.emeraldgrove.util.ControllerUtil;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.service.CollectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArticleControllerCollectionEndpointTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private CollectionService collectionService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ArticleController(articleService, collectionService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
    }

    @Test
    void getArticleCollectionIdsReturnsList() throws Exception {
        when(collectionService.getArticleCollectionIds("article-1", 1L)).thenReturn(List.of("collection-1", "collection-2"));

        mockMvc.perform(get("/api/articles/article-1/collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("collection-1"))
            .andExpect(jsonPath("$[1]").value("collection-2"));
    }
}