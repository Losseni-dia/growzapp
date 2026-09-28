-- Hash déterministe du numéro de pièce KYC, pour détecter les doublons
-- (même document utilisé sur plusieurs comptes) — impossible avec le seul
-- champ chiffré kyc_numero_piece (IV aléatoire AES-GCM, valeur différente
-- à chaque écriture même pour un numéro identique).
ALTER TABLE users ADD COLUMN kyc_numero_piece_hash VARCHAR(64);

CREATE INDEX idx_users_kyc_numero_piece_hash ON users(kyc_numero_piece_hash)
    WHERE kyc_numero_piece_hash IS NOT NULL;
