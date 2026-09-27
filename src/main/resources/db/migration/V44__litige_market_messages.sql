CREATE TABLE commande_market_litige_messages (
    id BIGSERIAL PRIMARY KEY,
    commande_id BIGINT NOT NULL REFERENCES commandes_market(id) ON DELETE CASCADE,
    auteur_id BIGINT NOT NULL REFERENCES users(id),
    role VARCHAR(20) NOT NULL,
    contenu VARCHAR(1000) NOT NULL,
    date_envoi TIMESTAMP NOT NULL
);

CREATE INDEX idx_commande_market_litige_messages_commande ON commande_market_litige_messages(commande_id);
