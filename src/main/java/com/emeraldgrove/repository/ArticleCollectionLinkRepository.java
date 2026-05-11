package com.emeraldgrove.repository;

import com.emeraldgrove.entity.ArticleCollectionLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleCollectionLinkRepository extends JpaRepository<ArticleCollectionLink, Long> {
    Optional<ArticleCollectionLink> findByArticleExternalIdAndCollectionExternalId(
        String articleExternalId,
        String collectionExternalId
    );

    @Query("""
        select link.article.externalId
        from ArticleCollectionLink link
        where link.collection.externalId = :collectionExternalId
        order by link.createdAt asc
    """)
    List<String> findArticleExternalIdsByCollectionExternalId(@Param("collectionExternalId") String collectionExternalId);

    @Query("""
        select link.collection.externalId
        from ArticleCollectionLink link
        where link.article.externalId = :articleExternalId
        order by link.createdAt asc
    """)
    List<String> findCollectionExternalIdsByArticleExternalId(@Param("articleExternalId") String articleExternalId);

    void deleteByArticleExternalIdAndCollectionExternalId(String articleExternalId, String collectionExternalId);
}
