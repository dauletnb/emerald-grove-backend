package com.emeraldgrove.entity;

import java.util.ArrayList;
import java.util.List;

import com.emeraldgrove.entity.baseEntity.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "article", schema = "emerald_grove")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 36)
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite = false;

    @Builder.Default
    @Column(name = "is_read_later", nullable = false)
    private boolean isReadLater = false;

    @Column(name = "ai_status", length = 20)
    private String aiStatus;

    @OneToMany(mappedBy = "article", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ArticleNote> notes = new ArrayList<>();

    @OneToMany(mappedBy = "article")
    @Builder.Default
    private List<AiJob> aiJobs = new ArrayList<>();

    @OneToMany(mappedBy = "article")
    @Builder.Default
    private List<AiResult> aiResults = new ArrayList<>();

    @OneToMany(mappedBy = "article")
    @Builder.Default
    private List<ArticleCollectionLink> collectionLinks = new ArrayList<>();
}
