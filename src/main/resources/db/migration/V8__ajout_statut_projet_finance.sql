ALTER TABLE projets DROP CONSTRAINT projets_statut_projet_check;

ALTER TABLE projets ADD CONSTRAINT projets_statut_projet_check
    CHECK (((statut_projet)::text = ANY ((ARRAY[
        'EN_PREPARATION'::character varying,
        'SOUMIS'::character varying,
        'VALIDE'::character varying,
        'REJETE'::character varying,
        'EN_COURS'::character varying,
        'TERMINE'::character varying,
        'EN_ATTENTE'::character varying,
        'FINANCE'::character varying
    ])::text[])));
