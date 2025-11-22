CREATE TABLE `documents` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `nom` VARCHAR(200) NOT NULL,
    `url` TEXT NOT NULL,
    `type` VARCHAR(50),
    `uploaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `projet_id` BIGINT NOT NULL,
    CONSTRAINT `fk_document_projet` 
        FOREIGN KEY (`projet_id`) REFERENCES `projets`(`id`) 
);