CREATE TABLE fiche_projets_mis_en_avant (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    projet_id BIGINT NOT NULL
);

CREATE INDEX idx_fiche_projets_mis_en_avant_user ON fiche_projets_mis_en_avant(user_id);
