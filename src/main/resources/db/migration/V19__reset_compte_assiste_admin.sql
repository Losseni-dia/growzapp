-- Réinitialisation de compte assistée par l'admin — mécanisme de secours
-- quand l'utilisateur n'a pas d'accès email fiable (contexte fréquent en
-- Afrique). L'admin vérifie l'identité hors application (téléphone/WhatsApp,
-- comparaison avec les documents KYC déjà en base) puis déclenche la
-- réinitialisation avec un motif de vérification obligatoire (traçabilité).

ALTER TABLE users ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN password_reset_at TIMESTAMP;
ALTER TABLE users ADD COLUMN password_reset_by VARCHAR(150);
ALTER TABLE users ADD COLUMN password_reset_motif VARCHAR(500);
