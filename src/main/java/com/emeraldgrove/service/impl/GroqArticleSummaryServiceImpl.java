package com.emeraldgrove.service.impl;

import com.emeraldgrove.constants.ErrorMessages;
import com.emeraldgrove.constants.GroqConstants;
import com.emeraldgrove.constants.LogMessages;
import com.emeraldgrove.dto.AiResponseDto;
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
public class GroqArticleSummaryServiceImpl implements ArticleSummaryService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean groqEnabled;
    private final String groqApiUrl;
    private final String groqApiKey;
    private final String groqModel;
    private final int groqSummaryMaxChars;
    private final int groqRequestTimeoutMs;

    public GroqArticleSummaryServiceImpl(
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
            .connectTimeout(Duration.ofSeconds(GroqConstants.CONNECT_TIMEOUT_SECONDS))
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
            log.info(LogMessages.LOG_GROQ_SUMMARY_SKIPPED_BLANK);
            return normalizedCurrent;
        }

        if (normalizedSource.equals(normalizedCurrent)) {
            log.info(LogMessages.LOG_GROQ_SUMMARY_SKIPPED_UNCHANGED);
            return normalizedCurrent;
        }

        if (!groqEnabled) {
            log.info(LogMessages.LOG_GROQ_SUMMARY_SKIPPED_DISABLED);
            return trimToLimit(normalizedSource);
        }

        if (groqApiKey.isBlank()) {
            log.warn(LogMessages.LOG_GROQ_SUMMARY_SKIPPED_NO_KEY);
            return trimToLimit(normalizedSource);
        }

        try {
            log.info(LogMessages.LOG_GROQ_SUMMARY_REQUEST_STARTED, groqModel, normalizedSource.length());
            String generatedSummary = requestSummary(title, normalizedSource);
            if (!generatedSummary.isBlank()) {
                log.info(LogMessages.LOG_GROQ_SUMMARY_REQUEST_SUCCEEDED, generatedSummary.length());
                return trimToLimit(generatedSummary);
            }
            log.warn(LogMessages.LOG_GROQ_SUMMARY_RESPONSE_EMPTY);
        } catch (Exception exception) {
            log.warn(LogMessages.LOG_GROQ_SUMMARY_GENERATION_FAILED, exception.getMessage());
        }

        return trimToLimit(normalizedSource);
    }

    private String requestSummary(String title, String sourceDescription) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            GroqConstants.JSON_KEY_MODEL, groqModel,
            GroqConstants.JSON_KEY_TEMPERATURE, GroqConstants.TEMPERATURE_SUMMARY,
            GroqConstants.JSON_KEY_MAX_COMPLETION_TOKENS, GroqConstants.MAX_COMPLETION_TOKENS_SUMMARY,
            GroqConstants.JSON_KEY_TOP_P, GroqConstants.TOP_P,
            GroqConstants.JSON_KEY_STREAM, GroqConstants.STREAM,
            GroqConstants.JSON_KEY_REASONING_EFFORT, GroqConstants.JSON_VALUE_REASONING_EFFORT_NONE,
            GroqConstants.JSON_KEY_REASONING_FORMAT, GroqConstants.JSON_VALUE_REASONING_FORMAT_HIDDEN,
            GroqConstants.JSON_KEY_MESSAGES, new Object[] {
                Map.of(
                    GroqConstants.JSON_KEY_ROLE, GroqConstants.JSON_VALUE_ROLE_USER,
                    GroqConstants.JSON_KEY_CONTENT, String.format(GroqConstants.PROMPT_SUMMARY, groqSummaryMaxChars, normalize(title), sourceDescription)
                )
            }
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(groqApiUrl))
            .timeout(Duration.ofMillis(groqRequestTimeoutMs))
            .header("Authorization", GroqConstants.HTTP_HEADER_AUTHORIZATION_PREFIX + groqApiKey)
            .header(GroqConstants.HTTP_HEADER_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(String.format(ErrorMessages.ERROR_GROQ_API_STATUS, response.statusCode(), response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path(GroqConstants.JSON_KEY_CHOICES).path(0).path(GroqConstants.JSON_KEY_MESSAGE).path(GroqConstants.JSON_KEY_CONTENT);
        return normalize(contentNode.asText(""));
    }

    public AiResponseDto analyzeArticle(String title, String description) throws IOException, InterruptedException {
        log.info(LogMessages.LOG_GROQ_ANALYSIS_REQUEST_STARTED, groqModel, description.length());
        AiResponseDto result = requestAnalysis(title, description);
        log.info(LogMessages.LOG_GROQ_ANALYSIS_REQUEST_SUCCEEDED, result.json().length(), result.tokens());
        return result;
    }

    public String getModel() {
        return groqModel;
    }

    private AiResponseDto requestAnalysis(String title, String description) throws IOException, InterruptedException {
        String prompt = String.format(GroqConstants.PROMPT_ANALYSIS, normalize(title), normalize(description));

        String requestBody = objectMapper.writeValueAsString(Map.of(
            GroqConstants.JSON_KEY_MODEL, groqModel,
            GroqConstants.JSON_KEY_TEMPERATURE, GroqConstants.TEMPERATURE_ANALYSIS,
            GroqConstants.JSON_KEY_MAX_COMPLETION_TOKENS, GroqConstants.MAX_COMPLETION_TOKENS_ANALYSIS,
            GroqConstants.JSON_KEY_TOP_P, GroqConstants.TOP_P,
            GroqConstants.JSON_KEY_STREAM, GroqConstants.STREAM,
            GroqConstants.JSON_KEY_REASONING_EFFORT, GroqConstants.JSON_VALUE_REASONING_EFFORT_NONE,
            GroqConstants.JSON_KEY_REASONING_FORMAT, GroqConstants.JSON_VALUE_REASONING_FORMAT_HIDDEN,
            GroqConstants.JSON_KEY_MESSAGES, new Object[]{
                Map.of(GroqConstants.JSON_KEY_ROLE, GroqConstants.JSON_VALUE_ROLE_USER, GroqConstants.JSON_KEY_CONTENT, prompt)
            }
        ));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(groqApiUrl))
            .timeout(Duration.ofMillis(groqRequestTimeoutMs))
            .header("Authorization", GroqConstants.HTTP_HEADER_AUTHORIZATION_PREFIX + groqApiKey)
            .header(GroqConstants.HTTP_HEADER_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(String.format(ErrorMessages.ERROR_GROQ_API_STATUS, response.statusCode(), response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        int tokensUsed = root.path(GroqConstants.JSON_KEY_USAGE).path(GroqConstants.JSON_KEY_TOTAL_TOKENS).asInt(0);
        String content = normalize(root.path(GroqConstants.JSON_KEY_CHOICES).path(0).path(GroqConstants.JSON_KEY_MESSAGE).path(GroqConstants.JSON_KEY_CONTENT).asText(""));

        return new AiResponseDto(content, tokensUsed);
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
