package com.emeraldgrove.controller;

import com.emeraldgrove.entity.User;
import com.emeraldgrove.exception.GlobalExceptionHandler;
import com.emeraldgrove.service.ArticleService;
import com.emeraldgrove.util.ControllerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new NoteController(articleService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        lenient().when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
    }

    @Test
    void deleteNoteCallsService() throws Exception {
        mockMvc.perform(delete("/api/articles/article-1/notes/note-1"))
            .andExpect(status().isNoContent());

        verify(articleService).deleteNote("article-1", "note-1", 1L);
    }
}
