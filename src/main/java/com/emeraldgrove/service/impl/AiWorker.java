package com.emeraldgrove.service.impl;

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

    @Scheduled(fixedDelay = 3000)
    @Transactional(noRollbackFor = Exception.class)
    public void processQueue() {
        List<AiJob> jobs = aiJobRepository.findTop10ByStatus("PENDING");

        for (AiJob job : jobs) {
            if (job.getRetries() >= 3) {
                job.setStatus("PERMANENT_FAILED");
                job.getArticle().setAiStatus("FAILED");
                aiJobRepository.save(job);
                continue;
            }

            try {
                job.setStatus("PROCESSING");
                aiJobRepository.save(job);

                orchestrator.processJob(job);

                job.setStatus("DONE");
                job.getArticle().setAiStatus("DONE");
            } catch (Exception e) {
                log.error("AI job {} failed (attempt {}): {}", job.getId(), job.getRetries() + 1, e.getMessage());
                job.setRetries(job.getRetries() + 1);
                job.setStatus("FAILED");
                job.getArticle().setAiStatus("FAILED");
            }

            aiJobRepository.save(job);
        }
    }
}
