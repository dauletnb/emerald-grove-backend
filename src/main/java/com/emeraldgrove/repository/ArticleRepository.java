package com.emeraldgrove.repository;

import com.emeraldgrove.entity.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "notes")
    Optional<Article> findByExternalIdAndUserId(String externalId, Long userId);

    @EntityGraph(attributePaths = "notes")
    Optional<Article> findByUrlAndUserId(String url, Long userId);

    @EntityGraph(attributePaths = "notes")
    List<Article> findAllByUserId(Long userId);
}
