CREATE TABLE wallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    solde_disponible DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    solde_bloque DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_wallet_user UNIQUE (user_id)
);
CREATE INDEX idx_wallet_user_id ON wallets(user_id);