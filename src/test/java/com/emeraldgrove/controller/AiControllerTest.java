package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ai.AiAnalysisResultResponseDton;
import com.emeraldgrove.entity.User;
import com.emeraldgrove.exception.GlobalExceptionHandler;
import com.emeraldgrove.service.AiService;
import com.emeraldgrove.util.ControllerUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    @Mock
    private AiService aiService;

    @Mock
    private ControllerUtil controllerUtil;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new AiController(aiService, controllerUtil))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        lenient().when(controllerUtil.getCurrentUser()).thenReturn(User.builder().id(1L).email("user@example.com").build());
    }

    @Test
    void getAiResultReturnsAiStatusAndContent() throws Exception {
        when(aiService.getAiResult(eq("article-1"), eq(1L)))
            .thenReturn(new AiAnalysisResultResponseDton("COMPLETED", null));

        mockMvc.perform(get("/api/ai/articles/article-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiStatus").value("COMPLETED"));
    }

    @Test
    void getAiResultWithLegacyPathReturnsAiStatusAndContent() throws Exception {
        when(aiService.getAiResult(eq("article-1"), eq(1L)))
            .thenReturn(new AiAnalysisResultResponseDton("COMPLETED", null));

        mockMvc.perform(get("/api/ai/articles/article-1/ai"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.aiStatus").value("COMPLETED"));
    }

    @Test
    void retryAiAnalysisCallsService() throws Exception {
        mockMvc.perform(post("/api/ai/articles/article-1/retry"))
            .andExpect(status().isAccepted());

        verify(aiService).retryAiAnalysis("article-1", 1L);
    }
}
