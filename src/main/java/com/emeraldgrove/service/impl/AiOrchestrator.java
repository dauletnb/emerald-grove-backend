package com.emeraldgrove.service.impl;

import com.emeraldgrove.dto.AiResponse;
import com.emeraldgrove.dto.AiResultDto;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.entity.AiResult;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.repository.AiResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiOrchestrator {

    private final GroqArticleSummaryService groqService;
    private final AiResultRepository aiResultRepository;
    private final ObjectMapper objectMapper;

    public void processJob(AiJob job) {
        switch (job.getType()) {
            case AiJob.TYPE_FULL_ANALYSIS -> processFull(job);
            default -> throw new IllegalArgumentException("Unknown job type: " + job.getType());
        }
    }

    private void processFull(AiJob job) {
        Article article = job.getArticle();

        String content = article.getDescription();
        if (content != null && content.length() > 8000) {
            content = content.substring(0, 8000);
        }

        AiResponse aiResponse;
        try {
            aiResponse = groqService.analyzeArticle(article.getTitle(), content);
        } catch (Exception e) {
            throw new RuntimeException("Groq analysis failed: " + e.getMessage(), e);
        }

        AiResultDto result = validate(aiResponse.json());

        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize AI result", e);
        }

        aiResultRepository.save(new AiResult(
            article,
            AiJob.TYPE_FULL_ANALYSIS,
            contentJson,
            groqService.getModel(),
            aiResponse.tokens()
        ));

        log.info("AI result saved for article {}, tokensUsed={}", article.getId(), aiResponse.tokens());
    }

    private AiResultDto validate(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("(?s)^```[a-zA-Z]*\\s*", "").replaceAll("(?s)```\\s*$", "").trim();
            }
            return objectMapper.readValue(cleaned, AiResultDto.class);
        } catch (Exception e) {
            log.error("AI response validation failed, using empty result: {}", e.getMessage());
            return new AiResultDto();
        }
    }
}
