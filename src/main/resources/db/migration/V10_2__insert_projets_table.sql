INSERT INTO `projets` (
    `id`, `poster`, `reference`, `libelle`, `description`, `valuation`, `roi_projete`,
    `parts_disponible`, `parts_prises`, `prix_une_part`, `objectif_financement`, `montant_collecte`,
    `financement_debut`, `financement_fin`, `valeur_totale_parts_en_pourcent`,
    `statut_projet`, `created_at`, `user_id`, `site_id`, `secteur_id`
) VALUES
(1, '/posters/solaire-dakar.jpg', 1001, 'Ferme Solaire Dakar', 'Projet d\'agriculture solaire à Dakar', 50000.0, 15.5, 1000, 50, 50.0, 50000.0, 2500.0, '2025-06-01 00:00:00', '2025-12-01 00:00:00', 100.0, 'EN_COURS', '2025-01-15 10:00:00', 1, 1, 1),
(2, '/posters/cacao-abidjan.jpg', 1002, 'EcoCacao Abidjan', 'Plantation durable de cacao', 75000.0, 20.0, 750, 30, 100.0, 75000.0, 3000.0, '2025-07-01 00:00:00', '2026-01-01 00:00:00', 100.0, 'EN_COURS', '2025-02-01 14:30:00', 2, 2, 8),
(3, '/posters/techvillage-accra.jpg', 1003, 'TechVillage Accra', 'Village tech pour startups', 100000.0, 25.0, 500, 100, 200.0, 100000.0, 20000.0, '2025-08-01 00:00:00', '2026-02-01 00:00:00', 100.0, 'EN_COURS', '2025-03-10 09:15:00', 3, 3, 4),
(4, '/posters/oilgreen-lagos.jpg', 1004, 'OilGreen Lagos', 'Énergie verte au Nigeria', 200000.0, 18.0, 2000, 80, 100.0, 200000.0, 8000.0, '2025-09-01 00:00:00', '2026-03-01 00:00:00', 100.0, 'EN_COURS', '2025-04-05 11:00:00', 4, 4, 2),
(5, '/posters/cottonbio-bamako.jpg', 1005, 'CottonBio Bamako', 'Coton biologique au Mali', 30000.0, 12.0, 600, 20, 50.0, 30000.0, 1000.0, '2025-05-01 00:00:00', '2025-11-01 00:00:00', 100.0, 'TERMINE', '2025-05-20 08:45:00', 5, 5, 1),
(6, '/posters/burkina-solar.jpg', 1006, 'BurkinaSolar', 'Panneaux solaires ruraux', 45000.0, 22.0, 900, 40, 50.0, 45000.0, 2000.0, '2025-10-01 00:00:00', '2026-04-01 00:00:00', 100.0, 'EN_PREPARATION', '2025-06-12 13:20:00', 6, 6, 2),
(7, '/posters/portbenin-eco.jpg', 1007, 'PortBenin Eco', 'Port durable à Cotonou', 80000.0, 16.0, 800, 60, 100.0, 80000.0, 6000.0, '2025-11-01 00:00:00', '2026-05-01 00:00:00', 100.0, 'EN_COURS', '2025-07-08 10:10:00', 7, 7, 10),
(8, '/posters/togo-agritech.jpg', 1008, 'TogoAgriTech', 'Tech agricole au Togo', 60000.0,  19.0, 600, 70, 100.0, 60000.0, 7000.0, '2025-12-01 00:00:00', '2026-06-01 00:00:00', 100.0, 'EN_COURS', '2025-08-03 15:00:00', 8, 8, 4),
(9, '/posters/guinee-mine-green.jpg', 1009, 'GuinéeMine Green', 'Exploitation minière verte', 150000.0, 14.0, 750, 200, 200.0, 150000.0, 40000.0, '2025-06-01 00:00:00', '2025-12-01 00:00:00', 100.0, 'TERMINE', '2025-09-18 09:30:00', 9, 9, 12),
(10, '/posters/sierra-tourism.jpg', 1010, 'SierraTourism', 'Tourisme éco à Freetown', 40000.0, 21.0, 800, 25, 50.0, 40000.0, 1250.0, '2025-07-01 00:00:00', '2026-01-01 00:00:00', 100.0, 'EN_ATTENTE', '2025-10-01 12:00:00', 10, 10, 7),
(11, '/posters/liberia-health.jpg', 1011, 'LiberiaHealth', 'Clinique rurale', 55000.0, 17.0, 1100, 55, 50.0, 55000.0, 2750.0, '2025-12-01 00:00:00', '2026-06-01 00:00:00', 100.0, 'EN_ATTENTE', '2025-11-01 16:45:00', 11, 11, 5),
(12, '/posters/gambie-fish.jpg', 1012, 'GambieFish', 'Pêche durable', 35000.0, 13.0, 700, 35, 50.0, 35000.0, 1750.0, '2026-01-01 00:00:00', '2026-07-01 00:00:00', 100.0, 'EN_PREPARATION', '2025-11-11 18:00:00', 12, 12, 8);