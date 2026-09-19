-- Fiche de présentation du porteur (crédibilité professionnelle, distincte
-- du KYC qui couvre l'identité civile). Validée par un admin ; aucun projet
-- ne peut être soumis ni validé tant que la fiche de son porteur n'est pas
-- VALIDEE (voir ProjetService.requireFichePorteurValidee / changerStatut).

ALTER TABLE users ADD COLUMN fiche_bio TEXT;
ALTER TABLE users ADD COLUMN fiche_statut_juridique VARCHAR(20);
ALTER TABLE users ADD COLUMN fiche_raison_sociale VARCHAR(150);
ALTER TABLE users ADD COLUMN fiche_annees_experience INTEGER;
ALTER TABLE users ADD COLUMN fiche_projets_precedents TEXT;
ALTER TABLE users ADD COLUMN fiche_contact_telephone VARCHAR(30);
ALTER TABLE users ADD COLUMN fiche_contact_email VARCHAR(191);
ALTER TABLE users ADD COLUMN fiche_site_web VARCHAR(255);
ALTER TABLE users ADD COLUMN fiche_linkedin VARCHAR(255);
ALTER TABLE users ADD COLUMN fiche_reseaux_autres VARCHAR(255);
ALTER TABLE users ADD COLUMN fiche_statut VARCHAR(20) NOT NULL DEFAULT 'NON_SOUMISE';
ALTER TABLE users ADD COLUMN fiche_submitted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN fiche_validated_at TIMESTAMP;
ALTER TABLE users ADD COLUMN fiche_commentaire_rejet VARCHAR(1000);

ALTER TABLE users ADD CONSTRAINT users_fiche_statut_juridique_check
    CHECK (fiche_statut_juridique IS NULL OR fiche_statut_juridique IN ('INDIVIDUEL', 'SOCIETE'));

ALTER TABLE users ADD CONSTRAINT users_fiche_statut_check
    CHECK (fiche_statut IN ('NON_SOUMISE', 'EN_ATTENTE', 'VALIDEE', 'REJETEE'));
