package com.emeraldgrove.repository;

import com.emeraldgrove.entity.AiJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiJobRepository extends JpaRepository<AiJob, UUID> {
    List<AiJob> findTop10ByStatus(String status);

    boolean existsByArticleIdAndType(Long articleId, String type);

    Optional<AiJob> findTopByArticleIdAndTypeOrderByCreatedAtDesc(Long articleId, String type);
}
