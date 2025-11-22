INSERT INTO `employes` (`id`, `nom`, `prenom`, `email`, `telephone`) VALUES
(1, 'Dia', 'Losseni', 'losseni.dia@growzapp.com', '+225 07 89 01 23'),
(2, 'Koffi', 'Awa', 'awa.koffi@growzapp.com', '+225 05 12 34 56'),
(3, 'Traoré', 'Moussa', 'moussa.traore@growzapp.com', '+225 01 23 45 67'),
(4, 'Ndiaye', 'Fatou', 'fatou.ndiaye@growzapp.com', '+225 07 77 88 99'),
(5, 'Konaté', 'Ibrahim', 'ibrahim.konate@growzapp.com', '+225 05 55 66 77')
ON DUPLICATE KEY UPDATE `telephone` = VALUES(`telephone`);