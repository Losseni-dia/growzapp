CREATE TABLE `employe_fonctions` (
    `employe_id` BIGINT NOT NULL,
    `fonction_id` BIGINT NOT NULL,
    `date_prise_fonction` DATE NOT NULL,
    PRIMARY KEY (`employe_id`, `fonction_id`),
    CONSTRAINT `fk_employe_fonctions_employe` 
        FOREIGN KEY (`employe_id`) REFERENCES `employes`(`id`),
    CONSTRAINT `fk_employe_fonctions_fonction` 
        FOREIGN KEY (`fonction_id`) REFERENCES `fonctions`(`id`)
);

CREATE INDEX `idx_employe_fonctions_date` ON `employe_fonctions`(`date_prise_fonction`);