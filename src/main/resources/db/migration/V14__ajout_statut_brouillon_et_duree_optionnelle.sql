-- Ajoute le statut BROUILLON (formulaire de soumission de projet enregistré
-- par le porteur avant validation/soumission finale) et rend la durée du
-- projet optionnelle (NULL = durée indéterminée, déjà supporté par l'UI mais
-- jusqu'ici rejeté par la contrainte NOT NULL et la validation backend).

ALTER TABLE projets DROP CONSTRAINT projets_statut_projet_check;

ALTER TABLE projets ADD CONSTRAINT projets_statut_projet_check
    CHECK (((statut_projet)::text = ANY ((ARRAY[
        'BROUILLON'::character varying,
        'EN_PREPARATION'::character varying,
        'SOUMIS'::character varying,
        'VALIDE'::character varying,
        'REJETE'::character varying,
        'EN_COURS'::character varying,
        'TERMINE'::character varying,
        'EN_ATTENTE'::character varying,
        'FINANCE'::character varying
    ])::text[])));

ALTER TABLE projets ALTER COLUMN duree_mois DROP NOT NULL;

-- La description était limitée à 255 caractères, largement insuffisant pour
-- un pitch de projet (constaté avec un texte de test de 988 caractères qui
-- aurait échoué à l'insertion avec une troncature SQL).
ALTER TABLE projets ALTER COLUMN description TYPE VARCHAR(5000);
