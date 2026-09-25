CREATE TABLE news_traductions (
    id BIGSERIAL PRIMARY KEY,
    news_id BIGINT NOT NULL REFERENCES news(id) ON DELETE CASCADE,
    langue VARCHAR(5) NOT NULL,
    title VARCHAR(300),
    content TEXT,
    CONSTRAINT uk_news_traductions_news_langue UNIQUE (news_id, langue)
);

CREATE INDEX idx_news_traductions_news ON news_traductions(news_id);
