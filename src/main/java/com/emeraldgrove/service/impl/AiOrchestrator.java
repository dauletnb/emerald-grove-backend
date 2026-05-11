package com.emeraldgrove.service.impl;

import org.springframework.stereotype.Service;

import com.emeraldgrove.constants.AiConstants;
import com.emeraldgrove.constants.ErrorMessages;
import com.emeraldgrove.dto.AiResponseDto;
import com.emeraldgrove.dto.AiResultDto;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.entity.AiResult;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.repository.AiResultRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiOrchestrator {

    private final GroqArticleSummaryServiceImpl groqService;
    private final AiResultRepository aiResultRepository;
    private final ObjectMapper objectMapper;

    public void processJob(AiJob job) {
        switch (job.getType()) {
            case AiJob.TYPE_FULL_ANALYSIS -> processFull(job);
            default -> throw new IllegalArgumentException(ErrorMessages.ERROR_UNKNOWN_JOB_TYPE.formatted(job.getType()));
        }
    }

    private void processFull(AiJob job) {
        Article article = job.getArticle();

        String content = article.getDescription();
        if (content != null && content.length() > AiConstants.MAX_CONTENT_LENGTH) {
            content = content.substring(0, AiConstants.MAX_CONTENT_LENGTH);
        }

        AiResponseDto aiResponseDto;
        try {
            aiResponseDto = groqService.analyzeArticle(article.getTitle(), content);
        } catch (Exception e) {
            throw new RuntimeException(ErrorMessages.ERROR_GROQ_ANALYSIS_FAILED.formatted(e.getMessage()), e);
        }

        AiResultDto result = validate(aiResponseDto.json());

        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException(ErrorMessages.ERROR_AI_RESULT_SERIALIZATION_FAILED, e);
        }

        aiResultRepository.save(new AiResult(
            article,
            AiJob.TYPE_FULL_ANALYSIS,
            contentJson,
            groqService.getModel(),
            aiResponseDto.tokens()
        ));

        log.info("AI result saved for article {}, tokensUsed={}", article.getId(), aiResponseDto.tokens());
    }

    private AiResultDto validate(String json) {
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith(AiConstants.MARKDOWN_CODE_BLOCK_START)) {
                cleaned = cleaned.replaceAll(AiConstants.MARKDOWN_CODE_BLOCK_PATTERN, "").replaceAll(AiConstants.MARKDOWN_CODE_BLOCK_END_PATTERN, "").trim();
            }
            return objectMapper.readValue(cleaned, AiResultDto.class);
        } catch (Exception e) {
            log.error("AI response validation failed, using empty result: {}", e.getMessage());
            return new AiResultDto();
        }
    }
}
