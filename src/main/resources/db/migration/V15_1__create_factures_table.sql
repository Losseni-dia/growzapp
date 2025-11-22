-- V16_1__create_factures_table.sql
-- Table factures avec backticks + CHECK corrigé

CREATE TABLE `factures` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `numero_facture` VARCHAR(50) NOT NULL UNIQUE,
    `montant_ht` DOUBLE PRECISION NOT NULL CHECK (`montant_ht` >= 0),
    `tva` DOUBLE PRECISION DEFAULT 0.0,
    `montant_ttc` DOUBLE PRECISION NOT NULL CHECK (`montant_ttc` >= 0),
    `date_emission` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `date_paiement` TIMESTAMP NULL,
    `statut` VARCHAR(20) DEFAULT 'EMISE',
    `dividende_id` BIGINT NOT NULL,
    `investisseur_id` BIGINT NOT NULL,
    `fichier_url` VARCHAR(50) NULL,

    CONSTRAINT `fk_facture_dividende`
        FOREIGN KEY (`dividende_id`) REFERENCES `dividendes`(`id`),

    CONSTRAINT `fk_facture_investisseur`
        FOREIGN KEY (`investisseur_id`) REFERENCES `users`(`id`)
        ON DELETE RESTRICT,

    CONSTRAINT `uk_facture_dividende` UNIQUE (`dividende_id`)
);