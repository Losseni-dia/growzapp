-- Une facture n'est plus forcément liée à un dividende : l'achat du statut
-- Premium par un porteur doit lui aussi produire un reçu/une facture.

ALTER TABLE factures ALTER COLUMN dividende_id DROP NOT NULL;

ALTER TABLE factures ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'DIVIDENDE';
ALTER TABLE factures ADD COLUMN projet_id BIGINT NULL REFERENCES projets(id);
ALTER TABLE factures ADD COLUMN libelle VARCHAR(255) NULL;
