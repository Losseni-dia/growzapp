-- Archivage personnel d'un document de projet, independant par utilisateur
-- (comme Gmail) : admin, porteur et investisseurs archivent chacun de leur
-- cote sans impact sur l'affichage des autres.
CREATE TABLE document_archives (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    archived_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_document_archives_document_user UNIQUE (document_id, user_id)
);

CREATE INDEX idx_document_archives_user_id ON document_archives(user_id);
