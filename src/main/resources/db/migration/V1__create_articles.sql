CREATE SCHEMA IF NOT EXISTS emerald_grove;

CREATE TABLE IF NOT EXISTS emerald_grove.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email
    ON emerald_grove.users (email);

CREATE TABLE IF NOT EXISTS emerald_grove.articles (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(36) UNIQUE,
    user_id BIGINT REFERENCES emerald_grove.users (id),
    title VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    description TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    ai_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_articles_external_id
    ON emerald_grove.articles (external_id)
    WHERE external_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_articles_user_url
    ON emerald_grove.articles (user_id, url)
    WHERE user_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS emerald_grove.article_notes (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    client_created_at BIGINT NOT NULL,
    article_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_article_notes_external_id UNIQUE (external_id),
    CONSTRAINT fk_article_notes_article
        FOREIGN KEY (article_id)
        REFERENCES emerald_grove.articles (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_article_notes_article_id
    ON emerald_grove.article_notes (article_id);

CREATE TABLE IF NOT EXISTS emerald_grove.ai_jobs (
    id UUID PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES emerald_grove.articles (id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retries INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_jobs_status
    ON emerald_grove.ai_jobs (status);

CREATE INDEX IF NOT EXISTS idx_ai_jobs_article_id
    ON emerald_grove.ai_jobs (article_id);

CREATE TABLE IF NOT EXISTS emerald_grove.ai_results (
    id UUID PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES emerald_grove.articles (id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    content JSONB,
    model VARCHAR(100),
    tokens_used INT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_results_article_id
    ON emerald_grove.ai_results (article_id);

CREATE TABLE IF NOT EXISTS emerald_grove.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES emerald_grove.users (id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_tokens_token
    ON emerald_grove.refresh_tokens (token);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON emerald_grove.refresh_tokens (user_id);
