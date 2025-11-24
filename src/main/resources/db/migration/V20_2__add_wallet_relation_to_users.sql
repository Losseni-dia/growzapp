-- V17__add_wallet_relation_to_users.sql

-- 1. On ajoute la colonne wallet_id (nullable au début pour ne pas bloquer)
ALTER TABLE users 
ADD COLUMN wallet_id BIGINT NULL;

-- 2. On ajoute la contrainte de clé étrangère + index
ALTER TABLE users 
ADD CONSTRAINT fk_users_wallet 
    FOREIGN KEY (wallet_id) REFERENCES wallets(id) 
    ON DELETE SET NULL;

CREATE INDEX idx_users_wallet_id ON users(wallet_id);

-- 3. On crée un wallet pour tous les users qui n'en ont pas encore
INSERT INTO wallets (user_id, solde_disponible, solde_bloque)
SELECT u.id, 0.00, 0.00
FROM users u
LEFT JOIN wallets w ON w.user_id = u.id
WHERE w.id IS NULL;

-- 4. On met à jour la colonne wallet_id avec l'ID du wallet correspondant
UPDATE users u
INNER JOIN wallets w ON u.id = w.user_id
SET u.wallet_id = w.id;

-- 5. Maintenant que tous les users ont un wallet → on passe la colonne en NOT NULL
ALTER TABLE users 
MODIFY COLUMN wallet_id BIGINT NOT NULL;

-- 6. On change la contrainte pour CASCADE (plus propre : si user supprimé → wallet supprimé)
ALTER TABLE users 
DROP FOREIGN KEY fk_users_wallet;

ALTER TABLE users 
ADD CONSTRAINT fk_users_wallet 
    FOREIGN KEY (wallet_id) REFERENCES wallets(id);