package com.emeraldgrove.controller;

import com.emeraldgrove.dto.ai.AiAnalysisResultResponseDton;
import com.emeraldgrove.service.AiService;
import com.emeraldgrove.util.ControllerUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI", description = "API для управления AI-аналитикой статей")
public class AiController {
    private final AiService aiService;
    private final ControllerUtil controllerUtil;

    @Operation(summary = "Получить результат анализа статьи")
    @GetMapping(value = {"/articles/{externalId}", "/articles/{externalId}/ai"})
    public ResponseEntity<AiAnalysisResultResponseDton> getAiResult(@PathVariable String externalId) {
        return ResponseEntity.ok(aiService.getAiResult(externalId, controllerUtil.getCurrentUser().getId()));
    }

    @Operation(summary = "Повторить анализ статьи")
    @PostMapping("/articles/{externalId}/retry")
    public ResponseEntity<Void> retryAiAnalysis(@PathVariable String externalId) {
        aiService.retryAiAnalysis(externalId, controllerUtil.getCurrentUser().getId());
        return ResponseEntity.accepted().build();
    }
}
