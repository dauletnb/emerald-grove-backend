package com.emeraldgrove.service.impl;

import com.emeraldgrove.service.ArticleSummaryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class GroqArticleSummaryService implements ArticleSummaryService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean groqEnabled;
    private final String groqApiUrl;
    private final String groqApiKey;
    private final String groqModel;
    private final int groqSummaryMaxChars;
    private final int groqRequestTimeoutMs;

    public GroqArticleSummaryService(
        ObjectMapper objectMapper,
        @Value("${emerald-grove.ai.groq.enabled:false}") boolean groqEnabled,
        @Value("${emerald-grove.ai.groq.api-url:https://api.groq.com/openai/v1/chat/completions}") String groqApiUrl,
        @Value("${emerald-grove.ai.groq.api-key:}") String groqApiKey,
        @Value("${emerald-grove.ai.groq.model:qwen/qwen3-32b}") String groqModel,
        @Value("${emerald-grove.ai.groq.summary-max-chars:280}") int groqSummaryMaxChars,
        @Value("${emerald-grove.ai.groq.request-timeout-ms:15000}") int groqRequestTimeoutMs
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        this.groqEnabled = groqEnabled;
        this.groqApiUrl = groqApiUrl;
        this.groqApiKey = groqApiKey;
        this.groqModel = groqModel;
        this.groqSummaryMaxChars = groqSummaryMaxChars;
        this.groqRequestTimeoutMs = groqRequestTimeoutMs;
    }

    @Override
    public String summarizeDescription(String title, String sourceDescription, String currentStoredDescription) {
        String normalizedSource = normalize(sourceDescription);
        String normalizedCurrent = normalize(currentStoredDescription);

        if (normalizedSource.isBlank()) {
            log.info("Groq summary skipped: source description is blank");
            return normalizedCurrent;
        }

        if (normalizedSource.equals(normalizedCurrent)) {
            log.info("Groq summary skipped: source description is unchanged");
            return normalizedCurrent;
        }

        if (!groqEnabled) {
            log.info("Groq summary skipped: emerald-grove.ai.groq.enabled=false");
            return trimToLimit(normalizedSource);
        }

        if (groqApiKey.isBlank()) {
            log.warn("Groq summary skipped: GROQ_API_KEY is empty or unavailable in the backend process");
            return trimToLimit(normalizedSource);
        }

        try {
            log.info("Groq summary request started: model={}, sourceLength={}", groqModel, normalizedSource.length());
            String generatedSummary = requestSummary(title, normalizedSource);
            if (!generatedSummary.isBlank()) {
                log.info("Groq summary request succeeded: summaryLength={}", generatedSummary.length());
                return trimToLimit(generatedSummary);
            }
            log.warn("Groq summary response was empty, using local fallback");
        } catch (Exception exception) {
            log.warn("Groq summary generation failed, using local fallback: {}", exception.getMessage());
        }

        return trimToLimit(normalizedSource);
    }

    private String requestSummary(String title, String sourceDescription) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", groqModel,
            "temperature", 0.3,
            "max_completion_tokens", 160,
            "top_p", 0.95,
            "stream", false,
            "reasoning_effort", "none",
            "reasoning_format", "hidden",
            "messages", new Object[] {
                Map.of(
                    "role", "user",
                    "content", """
                        Summarize this article in 1-2 short sentences.
                        Keep the summary under %d characters.
                        Use the same language as the source text.
                        Return plain text only.

                        Title: %s
                        Source text:
                        %s
                        """.formatted(groqSummaryMaxChars, normalize(title), sourceDescription)
                )
            }
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(groqApiUrl))
            .timeout(Duration.ofMillis(groqRequestTimeoutMs))
            .header("Authorization", "Bearer " + groqApiKey)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Groq API returned status " + response.statusCode() + " with body: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        return normalize(contentNode.asText(""));
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String trimToLimit(String value) {
        String normalized = normalize(value);
        if (normalized.length() <= groqSummaryMaxChars) {
            return normalized;
        }

        return normalized.substring(0, groqSummaryMaxChars).trim();
    }
}
