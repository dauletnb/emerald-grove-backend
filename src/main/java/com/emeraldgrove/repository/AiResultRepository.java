package com.emeraldgrove.repository;

import com.emeraldgrove.entity.AiResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiResultRepository extends JpaRepository<AiResult, UUID> {
    Optional<AiResult> findTopByArticleIdAndTypeOrderByCreatedAtDesc(Long articleId, String type);
}
