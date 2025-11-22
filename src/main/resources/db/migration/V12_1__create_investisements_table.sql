CREATE TABLE `investissements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `nombre_parts_pris` INTEGER NOT NULL CHECK (`nombre_parts_pris` > 0),
    `date` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `pourcent_equity` DOUBLE PRECISION DEFAULT 0.0,
    `frais` DOUBLE PRECISION DEFAULT 0.0,
    `statut_investissement` VARCHAR(50) DEFAULT 'EN_ATTENTE',
    `projet_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,

    CONSTRAINT `fk_investissement_projet`
        FOREIGN KEY (`projet_id`) REFERENCES `projets`(`id`) ON DELETE RESTRICT,

    CONSTRAINT `fk_investissement_investisseur`
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT
);


CREATE INDEX `idx_investissement_projet` ON `investissements`(`projet_id`);
CREATE INDEX `idx_investissement_user` ON `investissements`(`user_id`);
CREATE INDEX `idx_investissement_statut` ON `investissements`(`statut_investissement`);