CREATE TABLE contrats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    
    numero_contrat VARCHAR(50) NOT NULL UNIQUE,
    fichier_url VARCHAR(500),
    lien_verification VARCHAR(500),
    date_generation DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    investissement_id BIGINT NOT NULL UNIQUE,
    
    CONSTRAINT fk_contrat_investissement
        FOREIGN KEY (investissement_id) 
        REFERENCES investissements(id)
        ON DELETE CASCADE
);