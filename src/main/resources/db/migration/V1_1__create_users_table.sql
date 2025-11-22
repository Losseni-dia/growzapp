CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `image` VARCHAR(255),
    `login` VARCHAR(60) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `prenom` VARCHAR(60),
    `nom` VARCHAR(60),
    `email` VARCHAR(225) NOT NULL,
    `contact` VARCHAR(50),
    `localite_id` BIGINT,
    `sexe` VARCHAR(1) NOT NULL DEFAULT 'M' CHECK (`sexe` IN ('M', 'F')),

    CONSTRAINT `fk_user_localite` 
        FOREIGN KEY (`localite_id`) REFERENCES `localites`(`id`) ON DELETE SET NULL
);
