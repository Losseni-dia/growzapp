ALTER TABLE transactions
    ADD COLUMN source_paiement VARCHAR(20) NOT NULL DEFAULT 'WALLET_GROWZAPP';

-- Rétro-étiquetage best-effort des dépôts historiques d'après leur description
-- (les lignes de mouvement interne restent WALLET_GROWZAPP, valeur correcte).
UPDATE transactions
SET source_paiement = 'CARTE_BANCAIRE'
WHERE type = 'DEPOT' AND description ILIKE '%carte bancaire%';

UPDATE transactions
SET source_paiement = 'MOBILE_MONEY'
WHERE type = 'DEPOT' AND (
    description ILIKE '%mobile money%'
    OR description ILIKE '%orange money%'
    OR description ILIKE '%wave%'
    OR description ILIKE '%mtn%'
);
