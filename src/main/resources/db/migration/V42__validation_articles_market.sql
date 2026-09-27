ALTER TABLE articles_market ADD COLUMN statut_validation VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE';
ALTER TABLE articles_market ADD COLUMN motif_rejet VARCHAR(500);

-- Les articles deja publies avant l'ajout de cette validation restent
-- visibles (on ne les fait pas disparaitre retroactivement) ; seuls les
-- nouveaux articles et les modifications futures repassent par la validation.
UPDATE articles_market SET statut_validation = 'VALIDE';
