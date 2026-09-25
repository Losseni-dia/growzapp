CREATE TABLE fiche_bio_traductions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    langue VARCHAR(5) NOT NULL,
    bio TEXT,
    CONSTRAINT uk_fiche_bio_traductions_user_langue UNIQUE (user_id, langue)
);

CREATE INDEX idx_fiche_bio_traductions_user ON fiche_bio_traductions(user_id);
