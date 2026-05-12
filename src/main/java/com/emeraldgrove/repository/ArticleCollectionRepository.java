package com.emeraldgrove.repository;

import com.emeraldgrove.entity.ArticleCollection;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArticleCollectionRepository extends JpaRepository<ArticleCollection, Long> {
    Optional<ArticleCollection> findByExternalIdAndUserId(String externalId, Long userId);

    @EntityGraph(attributePaths = "articleLinks")
    Optional<ArticleCollection> findWithArticleLinksByExternalIdAndUserId(String externalId, Long userId);

    List<ArticleCollection> findAllByUserIdOrderByUpdatedAtDesc(Long userId);

    List<ArticleCollection> findAllByExternalIdInAndUserId(List<String> externalIds, Long userId);
}
