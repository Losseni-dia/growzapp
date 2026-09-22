ALTER TABLE articles_fournisseur
    ADD COLUMN photo_url VARCHAR(255);

-- Ville/pays/secteur peuvent désormais être vides le temps qu'un brouillon
-- de fiche fournisseur soit complété avant soumission.
ALTER TABLE fournisseurs
    ALTER COLUMN ville DROP NOT NULL,
    ALTER COLUMN pays DROP NOT NULL,
    ALTER COLUMN statut_juridique DROP NOT NULL;
