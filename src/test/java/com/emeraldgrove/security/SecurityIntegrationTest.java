package com.emeraldgrove.security;

import com.emeraldgrove.dto.SyncArticleNoteRequest;
import com.emeraldgrove.dto.SyncArticleRequest;
import com.emeraldgrove.enums.NoteType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Spring Security Integration Tests")
public class SecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final String VALID_EXTENSION_ID = "valid-extension-id-123";
    private static final String INVALID_EXTENSION_ID = "malicious-extension-id-456";
    private static final String VALID_TOKEN = "integration-test-extension-token";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @DisplayName("Should allow request with valid extension id and token")
    void shouldAllowRequestWithValidExtensionIdAndToken() throws Exception {
        SyncArticleRequest request = createValidSyncRequest();

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should block request with invalid extension id")
    void shouldBlockRequestWithInvalidExtensionId() throws Exception {
        SyncArticleRequest request = createValidSyncRequest();

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + INVALID_EXTENSION_ID)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow server-side test request without origin header when token is valid")
    void shouldAllowRequestWithoutOriginHeaderInServerSideTests() throws Exception {
        SyncArticleRequest request = createValidSyncRequest();

        mockMvc.perform(post("/api/articles/sync")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("Should block request with invalid token")
    void shouldBlockRequestWithInvalidToken() throws Exception {
        SyncArticleRequest request = createValidSyncRequest();

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", "chrome-extension://" + VALID_EXTENSION_ID)
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should block request with xss in origin")
    void shouldBlockRequestsWithXssInHeaders() throws Exception {
        SyncArticleRequest request = createValidSyncRequest();
        String maliciousOrigin = "chrome-extension://" + VALID_EXTENSION_ID + "<script>alert('xss')</script>";

        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", maliciousOrigin)
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    private SyncArticleRequest createValidSyncRequest() {
        return new SyncArticleRequest(
                "article-123",
                "https://example.com/test",
                "Тестовая статья",
                "Тестовое описание",
                List.of(
                        new SyncArticleNoteRequest(
                                "note-1",
                                NoteType.IDEA,
                                "Тестовая идея",
                                System.currentTimeMillis()
                        ),
                        new SyncArticleNoteRequest(
                                "note-2",
                                NoteType.TASK,
                                "Тестовая задача",
                                System.currentTimeMillis()
                        )
                )
        );
    }
}
