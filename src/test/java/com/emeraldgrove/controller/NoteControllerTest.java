package com.emeraldgrove.controller;

import com.emeraldgrove.dto.article.UpdateNoteRequestDto;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.enums.NoteType;
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

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new NoteController(articleService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        lenient().when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
        objectMapper = new ObjectMapper();
    }

    @Test
    void updateNoteCallsService() throws Exception {
        UpdateNoteRequestDto request = new UpdateNoteRequestDto(NoteType.QUESTION, "Updated text");

        mockMvc.perform(put("/api/articles/article-1/notes/note-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNoContent());

        verify(articleService).updateNote("article-1", "note-1", "Updated text", NoteType.QUESTION, 1L);
    }

    @Test
    void deleteNoteCallsService() throws Exception {
        mockMvc.perform(delete("/api/articles/article-1/notes/note-1"))
            .andExpect(status().isNoContent());

        verify(articleService).deleteNote("article-1", "note-1", 1L);
    }
}
