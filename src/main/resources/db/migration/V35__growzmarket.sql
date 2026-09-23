CREATE TABLE articles_market (
    id BIGSERIAL PRIMARY KEY,
    projet_id BIGINT NOT NULL REFERENCES projets(id),
    nom VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    prix NUMERIC(18,2) NOT NULL,
    unite VARCHAR(50) NOT NULL,
    disponible BOOLEAN NOT NULL DEFAULT TRUE,
    stock INTEGER,
    categorie VARCHAR(30) NOT NULL DEFAULT 'AUTRE',
    delai_preparation VARCHAR(100),
    point_retrait VARCHAR(255) NOT NULL,
    telephone_contact VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE articles_market_photos (
    article_id BIGINT NOT NULL REFERENCES articles_market(id) ON DELETE CASCADE,
    position INTEGER NOT NULL,
    url VARCHAR(255) NOT NULL,
    PRIMARY KEY (article_id, position)
);

CREATE TABLE commandes_market (
    id BIGSERIAL PRIMARY KEY,
    acheteur_id BIGINT NOT NULL REFERENCES users(id),
    projet_id BIGINT NOT NULL REFERENCES projets(id),
    montant_total NUMERIC(18,2) NOT NULL,
    statut VARCHAR(30) NOT NULL DEFAULT 'PAYEE',
    confirmation_lieu_retrait BOOLEAN NOT NULL DEFAULT FALSE,
    date_commande TIMESTAMP NOT NULL,
    date_prete TIMESTAMP,
    date_retrait_confirme TIMESTAMP,
    motif_litige VARCHAR(1000),
    facture_url VARCHAR(255)
);

CREATE TABLE commande_market_lignes (
    id BIGSERIAL PRIMARY KEY,
    commande_id BIGINT NOT NULL REFERENCES commandes_market(id) ON DELETE CASCADE,
    article_id BIGINT REFERENCES articles_market(id),
    libelle VARCHAR(150) NOT NULL,
    prix_unitaire NUMERIC(18,2) NOT NULL,
    quantite INTEGER NOT NULL,
    sous_total NUMERIC(18,2) NOT NULL
);

ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_type_check CHECK (type IN (
    'DEPOT', 'RETRAIT', 'TRANSFER_OUT', 'TRANSFER_IN', 'INVESTISSEMENT',
    'PAIEMENT_STRIPE', 'PAIEMENT_OM', 'PAIEMENT_MTN', 'PAIEMENT_WAVE', 'REMBOURSEMENT',
    'PAYOUT_OM', 'PAYOUT_MTN', 'PAYOUT_WAVE', 'PAYOUT_OM_SN', 'PAYOUT_WAVE_SN',
    'PAYOUT_MOOV', 'PAYOUT_STRIPE', 'PAYOUT_BANK',
    'CREDIT_PROJET', 'VIREMENT_PORTEUR', 'RETRAIT_MOBILE_MONEY',
    'VERSEMENT_PORTEUR', 'VERSEMENT_DIVIDENDE', 'DIVIDENDE_ENTRANT', 'DIVIDENDE_SORTANT',
    'DIVIDENDE', 'RETRAIT_ADMIN',
    'DEBLOCAGE_PROJET', 'TRANSFER_PROJET_VERS_PERSONNEL', 'RETRAIT_PROJET',
    'PREMIUM_PROJET', 'PAIEMENT_FOURNISSEUR', 'VENTE_MARKET'
));
