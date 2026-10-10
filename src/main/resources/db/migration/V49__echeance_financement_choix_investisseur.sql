-- Le statut ECHEC_FINANCEMENT (cloture d'un projet qui n'a pas atteint son
-- objectif a sa date limite) a ete ajoute a l'enum cote backend mais jamais
-- a la contrainte CHECK de la table projets — bug latent, corrige ici.
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
        'FINANCE'::character varying,
        'ECHEC_FINANCEMENT'::character varying
    ])::text[])));

-- Decision de l'investisseur pour le cycle d'echeance en cours (continuer /
-- recuperer son argent) une fois la date limite de financement du projet
-- depassee sans objectif atteint. Remis a NULL a chaque prolongation.
ALTER TABLE investissements ADD COLUMN choix_echeance_actuel VARCHAR(20);
ALTER TABLE investissements ADD CONSTRAINT investissements_choix_echeance_actuel_check
    CHECK (choix_echeance_actuel IS NULL OR choix_echeance_actuel IN ('CONTINUER', 'RECUPERER'));

-- Journal d'audit append-only de chaque decision individuelle (choisie par
-- l'investisseur OU declenchee par la cloture admin), avec le texte de
-- consentement exact affiche au moment de la decision — necessaire pour la
-- defensabilite juridique en cas de litige.
CREATE TABLE decision_echeance_investissement (
    id BIGSERIAL PRIMARY KEY,
    investissement_id BIGINT NOT NULL REFERENCES investissements(id),
    projet_id BIGINT NOT NULL REFERENCES projets(id),
    investisseur_id BIGINT NOT NULL REFERENCES users(id),
    choix VARCHAR(20) NOT NULL CHECK (choix IN ('CONTINUER', 'RECUPERER')),
    date_decision TIMESTAMP NOT NULL,
    date_fin_projet_au_moment DATE,
    consentement_texte VARCHAR(4000) NOT NULL,
    declenche_par_admin BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_decision_echeance_investissement_investissement_id
    ON decision_echeance_investissement(investissement_id);
CREATE INDEX idx_decision_echeance_investissement_projet_id
    ON decision_echeance_investissement(projet_id);
