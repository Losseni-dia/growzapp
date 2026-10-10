-- Canal de messagerie par projet entre admin/communicant et investisseurs,
-- porteur strictement exclu. Un message admin peut etre diffuse a tous les
-- investisseurs (aucune ligne dans projet_message_destinataires) ou cible
-- sur un sous-ensemble (une ligne par destinataire).
CREATE TABLE projet_messages (
    id BIGSERIAL PRIMARY KEY,
    projet_id BIGINT NOT NULL REFERENCES projets(id),
    auteur_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'INVESTISSEUR')),
    contenu VARCHAR(2000) NOT NULL,
    date_envoi TIMESTAMP NOT NULL
);

CREATE TABLE projet_message_destinataires (
    message_id BIGINT NOT NULL REFERENCES projet_messages(id),
    investisseur_id BIGINT NOT NULL
);

CREATE INDEX idx_projet_messages_projet_id ON projet_messages(projet_id);
CREATE INDEX idx_projet_message_destinataires_message_id ON projet_message_destinataires(message_id);
