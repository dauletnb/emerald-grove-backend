CREATE SCHEMA IF NOT EXISTS emerald_grove;

CREATE TABLE IF NOT EXISTS emerald_grove.user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_email
    ON emerald_grove.user (email);

CREATE TABLE IF NOT EXISTS emerald_grove.article (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(36) UNIQUE,
    user_id BIGINT REFERENCES emerald_grove.user (id),
    title VARCHAR(255) NOT NULL,
    url VARCHAR(2048) NOT NULL,
    description TEXT,
    ai_status VARCHAR(20),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_article_external_id
    ON emerald_grove.article (external_id)
    WHERE external_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_article_user_url
    ON emerald_grove.article (user_id, url)
    WHERE user_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS emerald_grove.article_note (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    client_created_at TIMESTAMP NOT NULL,
    article_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_article_note_external_id UNIQUE (external_id),
    CONSTRAINT fk_article_note_article
        FOREIGN KEY (article_id)
        REFERENCES emerald_grove.article (id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_article_note_article_id
    ON emerald_grove.article_note (article_id);

CREATE TABLE IF NOT EXISTS emerald_grove.ai_job (
    id UUID PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES emerald_grove.article (id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retries INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_job_status
    ON emerald_grove.ai_job (status);

CREATE INDEX IF NOT EXISTS idx_ai_job_article_id
    ON emerald_grove.ai_job (article_id);

CREATE TABLE IF NOT EXISTS emerald_grove.ai_result (
    id UUID PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES emerald_grove.article (id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    content JSONB,
    model VARCHAR(100),
    tokens_used INT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ai_result_article_id
    ON emerald_grove.ai_result (article_id);

CREATE TABLE IF NOT EXISTS emerald_grove.refresh_token (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES emerald_grove.user (id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_token_token
    ON emerald_grove.refresh_token (token);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id
    ON emerald_grove.refresh_token (user_id);
