CREATE TABLE emerald_grove.article_collection (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_collection_external_id UNIQUE (external_id),
    CONSTRAINT fk_collection_user
        FOREIGN KEY (user_id)
        REFERENCES emerald_grove.user (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_collection_user_id
    ON emerald_grove.article_collection (user_id);

CREATE TABLE emerald_grove.article_collection_link (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(64) NOT NULL,
    article_id BIGINT NOT NULL,
    collection_id BIGINT NOT NULL,
    client_created_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_article_collection_link_external_id UNIQUE (external_id),
    CONSTRAINT uk_article_collection_pair UNIQUE (article_id, collection_id),
    CONSTRAINT fk_link_article
        FOREIGN KEY (article_id)
        REFERENCES emerald_grove.article (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_link_collection
        FOREIGN KEY (collection_id)
        REFERENCES emerald_grove.article_collection (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_link_article_id
    ON emerald_grove.article_collection_link (article_id);

CREATE INDEX idx_link_collection_id
    ON emerald_grove.article_collection_link (collection_id);
