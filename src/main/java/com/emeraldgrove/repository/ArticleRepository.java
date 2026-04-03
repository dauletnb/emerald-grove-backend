package com.emeraldgrove.repository;

import com.emeraldgrove.entity.Article;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @EntityGraph(attributePaths = "notes")
    Optional<Article> findByUrl(String url);

    @EntityGraph(attributePaths = "notes")
    Optional<Article> findByExternalId(String externalId);

    @Override
    @EntityGraph(attributePaths = "notes")
    @NonNull
    List<Article> findAll();
}
