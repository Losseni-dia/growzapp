ALTER TABLE commandes_market ADD COLUMN reference_externe_stripe VARCHAR(255);
CREATE UNIQUE INDEX idx_commandes_market_reference_externe_stripe
    ON commandes_market(reference_externe_stripe)
    WHERE reference_externe_stripe IS NOT NULL;
