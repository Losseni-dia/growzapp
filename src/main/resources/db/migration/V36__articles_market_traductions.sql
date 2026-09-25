CREATE TABLE articles_market_traductions (
    id BIGSERIAL PRIMARY KEY,
    article_id BIGINT NOT NULL REFERENCES articles_market(id) ON DELETE CASCADE,
    langue VARCHAR(5) NOT NULL,
    nom VARCHAR(200),
    description TEXT,
    CONSTRAINT uk_article_traduction_article_langue UNIQUE (article_id, langue)
);
