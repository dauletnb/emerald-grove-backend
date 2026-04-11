package com.emeraldgrove.entity;

import com.emeraldgrove.entity.baseEntity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ai_job", schema = "emerald_grove")
@Getter
@Setter
@NoArgsConstructor
public class AiJob extends BaseEntity {
    public static final String TYPE_FULL_ANALYSIS = "FULL_ANALYSIS";

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id", nullable = false)
    private Article article;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private int retries = 0;

    public static AiJob create(Article article, String type) {
        AiJob job = new AiJob();
        job.setId(UUID.randomUUID());
        job.setArticle(article);
        job.setType(type);
        job.setStatus("PENDING");
        job.setRetries(0);
        return job;
    }

    public static AiJob createFullAnalysisJob(Article article) {
        return create(article, TYPE_FULL_ANALYSIS);
    }
}
