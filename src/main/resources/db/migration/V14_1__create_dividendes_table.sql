CREATE TABLE `dividendes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `montant_par_part` DOUBLE PRECISION NOT NULL,
    `statut_dividende` VARCHAR(50) DEFAULT 'PLANIFIE',
    `moyen_paiement` VARCHAR(50),
    `date_paiement` TIMESTAMP,
    `investissement_id` BIGINT NOT NULL,

    CONSTRAINT `fk_dividende_investissement` 
        FOREIGN KEY (`investissement_id`) REFERENCES `investissements`(`id`) 
);