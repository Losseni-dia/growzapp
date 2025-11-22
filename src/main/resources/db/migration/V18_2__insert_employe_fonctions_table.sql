INSERT INTO `employe_fonctions` (`employe_id`, `fonction_id`, `date_prise_fonction`) VALUES
(1, 1, '2025-01-15'), -- Losseni → Développeur
(1, 2, '2025-03-01'), -- Losseni → Manager
(2, 3, '2025-02-10'), -- Awa → Designer UX
(3, 4, '2025-01-20'), -- Moussa → Data Analyst
(4, 1, '2025-04-01'), -- Fatou → Développeur
(5, 5, '2025-03-15')  -- Ibrahim → DevOps
ON DUPLICATE KEY UPDATE `date_prise_fonction` = VALUES(`date_prise_fonction`);