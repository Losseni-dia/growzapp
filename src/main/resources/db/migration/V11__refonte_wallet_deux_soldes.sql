-- Refonte du wallet à deux soldes (soldeDisponible / soldeBloque).
-- solde_retirable n'est plus écrit par aucun code (validerRetrait/retirerGains/
-- createBankPayoutWithNewTransaction, seuls appelants historiques, ont été
-- supprimés) : vérifié en staging avant déploiement qu'aucune ligne n'a de
-- solde_retirable non nul.
ALTER TABLE wallets DROP COLUMN solde_retirable;

-- Traçabilité des nouveaux mouvements de trésorerie projet (déblocage admin,
-- retrait/transfert porteur) : réutilise la table transactions existante
-- plutôt qu'une table séparée.
ALTER TABLE transactions ADD COLUMN idempotency_key VARCHAR(255) UNIQUE;
ALTER TABLE transactions ADD COLUMN auteur_id BIGINT;
