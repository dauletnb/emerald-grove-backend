package com.emeraldgrove.controller;

import com.emeraldgrove.dto.CollectionDto;
import com.emeraldgrove.dto.CollectionRequestDto;
import com.emeraldgrove.dto.CollectionSyncDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.exception.GlobalExceptionHandler;
import com.emeraldgrove.util.ControllerUtil;
import com.emeraldgrove.service.CollectionService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CollectionControllerTest {

    @Mock
    private CollectionService collectionService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new CollectionController(collectionService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();

        lenient().when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
    }

    @Test
    void createCollectionReturnsCreatedPayload() throws Exception {
        when(collectionService.createCollection(any(CollectionRequestDto.class), eq(1L)))
            .thenReturn(new CollectionDto(1L, "collection-1", "Saved", 1000L, 2000L, 0, List.of()));

        mockMvc.perform(post("/api/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CollectionRequestDto("Saved"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.externalId").value("collection-1"))
            .andExpect(jsonPath("$.name").value("Saved"));
    }

    @Test
    void createCollectionValidatesBlankName() throws Exception {
        mockMvc.perform(post("/api/collections")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CollectionRequestDto(" "))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCollectionsReturnsList() throws Exception {
        when(collectionService.getAllCollections(1L))
            .thenReturn(List.of(new CollectionSyncDto("collection-1", "Saved")));

        mockMvc.perform(get("/api/collections"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].externalId").value("collection-1"))
            .andExpect(jsonPath("$[0].name").value("Saved"));
    }

    @Test
    void renameCollectionReturnsUpdatedPayload() throws Exception {
        when(collectionService.renameCollection(eq("collection-1"), any(CollectionRequestDto.class), eq(1L)))
            .thenReturn(new CollectionDto(1L, "collection-1", "Renamed", 1000L, 3000L, 1, List.of("article-1")));

        mockMvc.perform(put("/api/collections/collection-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CollectionRequestDto("Renamed"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Renamed"))
            .andExpect(jsonPath("$.articleCount").value(1));
    }

    @Test
    void addArticleToCollectionReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/collections/collection-1/articles/article-1"))
            .andExpect(status().isCreated());

        verify(collectionService).addArticleToCollection("article-1", "collection-1", 1L);
    }

    @Test
    void removeArticleFromCollectionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/collections/collection-1/articles/article-1"))
            .andExpect(status().isNoContent());

        verify(collectionService).removeArticleFromCollection("article-1", "collection-1", 1L);
    }

    @Test
    void getCollectionArticleIdsReturnsList() throws Exception {
        when(collectionService.getCollectionArticleIds("collection-1", 1L)).thenReturn(List.of(
            "article-1", "article-2"
        ));
        mockMvc.perform(get("/api/collections/collection-1/articles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0]").value("article-1"))
            .andExpect(jsonPath("$[1]").value("article-2"));
    }
}
