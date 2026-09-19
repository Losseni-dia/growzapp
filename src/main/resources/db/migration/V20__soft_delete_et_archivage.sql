-- Soft delete (masqué, restaurable, purge définitive réservée à l'admin)
-- pour les entités où une suppression complète a du sens.

ALTER TABLE users ADD COLUMN supprime_le TIMESTAMP NULL;
ALTER TABLE users ADD COLUMN supprime_par VARCHAR(150) NULL;
ALTER TABLE users ADD COLUMN motif_suppression VARCHAR(500) NULL;

ALTER TABLE projets ADD COLUMN supprime_le TIMESTAMP NULL;
ALTER TABLE projets ADD COLUMN supprime_par VARCHAR(150) NULL;
ALTER TABLE projets ADD COLUMN motif_suppression VARCHAR(500) NULL;

ALTER TABLE investissements ADD COLUMN supprime_le TIMESTAMP NULL;
ALTER TABLE investissements ADD COLUMN supprime_par VARCHAR(150) NULL;
ALTER TABLE investissements ADD COLUMN motif_suppression VARCHAR(500) NULL;

-- Archivage seul (jamais de suppression, ce sont des documents légaux)
-- pour contrats et factures.

ALTER TABLE contrats ADD COLUMN archive_le TIMESTAMP NULL;
ALTER TABLE contrats ADD COLUMN archive_par VARCHAR(150) NULL;

ALTER TABLE factures ADD COLUMN archive_le TIMESTAMP NULL;
ALTER TABLE factures ADD COLUMN archive_par VARCHAR(150) NULL;
