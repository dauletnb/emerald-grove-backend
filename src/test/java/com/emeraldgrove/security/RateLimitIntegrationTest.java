package com.emeraldgrove.security;

import com.emeraldgrove.dto.SyncArticleRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.List;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that rate limiting returns 429 after the configured limit is exceeded.
 * Uses low limits (3) via property overrides to keep the test fast.
 * Runs in a separate Spring context from BackendDataAcceptanceTest due to different properties.
 */
@SpringBootTest(properties = {
    "emerald-grove.rate-limit.sync-limit=3",
    "emerald-grove.rate-limit.auth-check-limit=3",
    "emerald-grove.rate-limit.window-seconds=60"
})
@ActiveProfiles("test")
@DisplayName("Rate Limit Integration Tests")
class RateLimitIntegrationTest {

    private static final String TOKEN = "integration-test-extension-token";
    private static final String ORIGIN = "chrome-extension://valid-extension-id-123";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /api/articles/sync должен вернуть 429 после превышения лимита")
    void shouldReturn429ForSyncAfterLimitExceeded() throws Exception {
        SyncArticleRequest validRequest = new SyncArticleRequest(
                "rate-limit-test",
                "https://example.com/rate-limit",
                "Rate Limit Test Article",
                "Test description",
                List.of()
        );
        String body = objectMapper.writeValueAsString(validRequest);

        // First 3 requests should succeed (limit = 3)
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/articles/sync")
                            .header("Origin", ORIGIN)
                            .header("Authorization", "Bearer " + TOKEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().is2xxSuccessful());
        }

        // 4th request should be rate-limited
        mockMvc.perform(post("/api/articles/sync")
                        .header("Origin", ORIGIN)
                        .header("Authorization", "Bearer " + TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests"))
                .andExpect(jsonPath("$.retryAfter").isNumber());
    }

    @Test
    @DisplayName("GET /api/auth/check должен вернуть 429 после превышения лимита")
    void shouldReturn429ForAuthCheckAfterLimitExceeded() throws Exception {
        // First 3 requests should succeed
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/api/auth/check")
                            .header("Origin", ORIGIN)
                            .header("Authorization", "Bearer " + TOKEN))
                    .andExpect(status().isOk());
        }

        // 4th request should be rate-limited
        mockMvc.perform(get("/api/auth/check")
                        .header("Origin", ORIGIN)
                        .header("Authorization", "Bearer " + TOKEN))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Too many requests"))
                .andExpect(jsonPath("$.retryAfter").isNumber());
    }
}
