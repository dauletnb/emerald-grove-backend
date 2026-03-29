package com.emeraldgrove.repository;

import com.emeraldgrove.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Optional<Article> findByUrl(String url);
    Optional<Article> findByExternalId(String externalId);
}