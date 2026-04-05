CREATE SCHEMA IF NOT EXISTS emerald_grove;

CREATE TABLE IF NOT EXISTS emerald_grove.articles (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(36) UNIQUE,
    title VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    description TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_articles_url
    ON emerald_grove.articles (url);

CREATE UNIQUE INDEX IF NOT EXISTS uk_articles_external_id
    ON emerald_grove.articles (external_id)
    WHERE external_id IS NOT NULL;
