package com.emeraldgrove.service.impl;

import com.emeraldgrove.constants.AiConstants;
import com.emeraldgrove.constants.AiStatusConstants;
import com.emeraldgrove.constants.LogMessages;
import com.emeraldgrove.entity.AiJob;
import com.emeraldgrove.repository.AiJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiWorker {
    private final AiJobRepository aiJobRepository;
    private final AiOrchestrator orchestrator;

    @Scheduled(fixedDelay = AiConstants.SCHEDULED_DELAY_MS)
    @Transactional(noRollbackFor = Exception.class)
    public void processQueue() {
        List<AiJob> jobs = aiJobRepository.findTop10ByStatus(AiStatusConstants.AI_STATUS_PENDING);

        for (AiJob job : jobs) {
            if (job.getRetries() >= AiConstants.MAX_RETRIES) {
                job.setStatus(AiStatusConstants.AI_STATUS_PERMANENT_FAILED);
                job.getArticle().setAiStatus(AiStatusConstants.AI_STATUS_FAILED);
                aiJobRepository.save(job);
                continue;
            }

            try {
                job.setStatus(AiStatusConstants.AI_STATUS_PROCESSING);
                aiJobRepository.save(job);

                orchestrator.processJob(job);

                job.setStatus(AiStatusConstants.AI_STATUS_DONE);
                job.getArticle().setAiStatus(AiStatusConstants.AI_STATUS_DONE);
            } catch (Exception e) {
                log.error(LogMessages.LOG_AI_JOB_FAILED, job.getId(), job.getRetries() + 1, e.getMessage());
                job.setRetries(job.getRetries() + 1);
                job.setStatus(AiStatusConstants.AI_STATUS_FAILED);
                job.getArticle().setAiStatus(AiStatusConstants.AI_STATUS_FAILED);
            }

            aiJobRepository.save(job);
        }
    }
}
