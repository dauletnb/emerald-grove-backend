CREATE SCHEMA IF NOT EXISTS emerald_grove;

CREATE TABLE IF NOT EXISTS emerald_grove.article_notes (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    CONSTRAINT uk_article_notes_external_id UNIQUE (external_id),
    CONSTRAINT fk_article_notes_article
        FOREIGN KEY (article_id)
        REFERENCES emerald_grove.articles (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_article_notes_article_id
    ON emerald_grove.article_notes (article_id);
