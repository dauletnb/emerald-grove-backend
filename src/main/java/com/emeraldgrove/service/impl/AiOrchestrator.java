package com.emeraldgrove.service.impl;

import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import com.emeraldgrove.constants.AiConstants;
import com.emeraldgrove.constants.ErrorMessages;
import com.emeraldgrove.dto.AiResponseDto;
import com.emeraldgrove.dto.AiResultDto;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.entity.AiResult;
import com.emeraldgrove.entity.Article;
import com.emeraldgrove.repository.AiResultRepository;
import com.emeraldgrove.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiOrchestrator {

    private final GroqArticleSummaryServiceImpl groqService;
    private final AiResultRepository aiResultRepository;
    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper;

    public void processJob(AiJob job) {
        switch (job.getType()) {
            case AiJob.TYPE_FULL_ANALYSIS -> processFull(job);
            default -> throw new IllegalArgumentException(ErrorMessages.ERROR_UNKNOWN_JOB_TYPE.formatted(job.getType()));
        }
    }

    private void processFull(AiJob job) {
        Article article = job.getArticle();

        String rawText = article.getContent() != null && !article.getContent().isBlank()
            ? article.getContent()
            : article.getDescription();

        String plainText = rawText != null ? Jsoup.parse(rawText).text() : "";

        String truncatedText = plainText.length() > AiConstants.MAX_CONTENT_LENGTH
            ? plainText.substring(0, AiConstants.MAX_CONTENT_LENGTH)
            : plainText;

        AiResponseDto aiResponseDto;
        try {
            aiResponseDto = groqService.analyzeArticle(article.getTitle(), truncatedText);
        } catch (Exception e) {
            throw new RuntimeException(ErrorMessages.ERROR_GROQ_ANALYSIS_FAILED.formatted(e.getMessage()), e);
        }

        AiResultDto result = validate(aiResponseDto.json());

        if (result.summary != null
                && result.summary.shortText != null
                && !result.summary.shortText.isBlank()) {
            article.setDescription(result.summary.shortText);
            articleRepository.save(article);
            log.info("AI short summary saved to article.description, articleId={}", article.getId());
        }

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
