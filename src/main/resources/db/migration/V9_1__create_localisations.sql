CREATE TABLE `localisations` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(150) NOT NULL,
    `adresse` TEXT,
    `contact` VARCHAR(100),
    `responsable` VARCHAR(100),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `localite_id` BIGINT,
    CONSTRAINT `fk_localisation_localite` 
        FOREIGN KEY (`localite_id`) REFERENCES `localites`(`id`) ON DELETE SET NULL
);