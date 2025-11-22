CREATE TABLE `localites` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `code_postal` VARCHAR(20),
    `nom` VARCHAR(100) NOT NULL,
    `pays_id` BIGINT,
    CONSTRAINT `fk_localite_pays` 
        FOREIGN KEY (`pays_id`) REFERENCES `pays`(`id`) ON DELETE SET NULL
);