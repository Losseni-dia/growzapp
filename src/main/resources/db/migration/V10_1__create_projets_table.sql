CREATE TABLE `projets` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `poster` VARCHAR(255),
    `reference` INTEGER,
    `libelle` VARCHAR(150) NOT NULL,
    `description` TEXT,
    `valuation` DOUBLE PRECISION DEFAULT 0.0,
    `roi_projete` DOUBLE PRECISION DEFAULT 0.0,
    `parts_disponible` INTEGER NOT NULL DEFAULT 0 CHECK (`parts_disponible` >= 0),
    `parts_prises` INTEGER NOT NULL DEFAULT 0 CHECK (`parts_prises` >= 0),
    `prix_une_part` DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (`prix_une_part` >= 0),
    `objectif_financement` DOUBLE PRECISION NOT NULL DEFAULT 0.0 CHECK (`objectif_financement` >= 0),
    `montant_collecte` DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    `financement_debut` TIMESTAMP NULL,
    `financement_fin` TIMESTAMP NULL,
    `valeur_totale_parts_en_pourcent` DOUBLE PRECISION DEFAULT 100.0,
    `statut_projet` VARCHAR(50) DEFAULT 'EN_PREPARATION',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `user_id` BIGINT NOT NULL,
    `site_id` BIGINT,
    `secteur_id` BIGINT NOT NULL,

    CONSTRAINT `fk_projet_porteur` 
        FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_projet_site` 
        FOREIGN KEY (`site_id`) REFERENCES `localisations`(`id`) ON DELETE SET NULL,
    CONSTRAINT `fk_projet_secteur` 
        FOREIGN KEY (`secteur_id`) REFERENCES `secteurs`(`id`) ON DELETE RESTRICT
);

-- Index pour performance
CREATE INDEX `idx_projet_statut` ON `projets`(`statut_projet`);
CREATE INDEX `idx_projet_porteur` ON `projets`(`user_id`);
CREATE INDEX `idx_projet_dates` ON `projets`(`financement_debut`, `financement_fin`);