package com.emeraldgrove.repository;

import com.emeraldgrove.entity.ArticleNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ArticleNoteRepository extends JpaRepository<ArticleNote, Long> {
    @Transactional
    void deleteByExternalIdAndArticleExternalId(String noteExternalId, String articleExternalId);
}
