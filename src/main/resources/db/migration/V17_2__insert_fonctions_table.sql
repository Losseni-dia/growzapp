-- V16__insert_fonctions_sample.sql
INSERT INTO `fonctions` (`id`, `nom`, `description`) VALUES
(1, 'Développeur', 'Développeur Java/Spring Boot'),
(2, 'Manager', 'Responsable d’équipe'),
(3, 'Designer UX', 'Conception d’interface utilisateur'),
(4, 'Data Analyst', 'Analyse de données'),
(5, 'DevOps', 'Gestion infrastructure cloud')
ON DUPLICATE KEY UPDATE `description` = VALUES(`description`);