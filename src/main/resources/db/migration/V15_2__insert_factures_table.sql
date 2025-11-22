INSERT INTO `factures` (
    `numero_facture`, `montant_ht`, `tva`, `montant_ttc`, `date_emission`, `date_paiement`, `statut`, `dividende_id`, `investisseur_id`,`fichier_url`
) VALUES
('FAC-2025-0002', 380.0, 0.0, 380.0, '2025-04-10 09:30:00', '2025-04-15 14:20:00', 'PAYEE', 2, 3, 'fact.pdf'),
('FAC-2025-0003', 150.0, 0.0, 150.0, '2025-05-05 11:15:00', NULL, 'EMISE', 3, 4, 'fact.pdf'),
('FAC-2025-0004', 720.0, 0.0, 720.0, '2025-06-12 13:45:00', '2025-06-18 10:00:00', 'PAYEE', 4, 5, 'fact.pdf'),
('FAC-2025-0005', 290.0, 0.0, 290.0, '2025-07-08 08:20:00', '2025-08-01 16:30:00', 'EN_RETARD', 5, 6, 'fact.pdf'),
('FAC-2025-0006', 1150.0, 0.0, 1150.0, '2025-08-03 15:10:00', '2025-08-05 09:00:00', 'PAYEE', 6, 7, 'fact.pdf'),
('FAC-2025-0007', 480.0, 0.0, 480.0, '2025-09-18 10:45:00', NULL, 'EMISE', 7, 8, 'fact.pdf'),
('FAC-2025-0008', 920.0, 0.0, 920.0, '2025-10-01 12:00:00', '2025-10-03 11:11:00', 'PAYEE', 8, 9, 'fact.pdf'),
('FAC-2025-0009', 310.0, 0.0, 310.0, '2025-11-01 16:20:00', NULL, 'EMISE', 9, 10, 'fact.pdf'),
('FAC-2025-0010', 650.0, 0.0, 650.0, '2025-11-11 18:30:00', '2025-11-12 09:00:00', 'PAYEE', 10, 11, 'fact.pdf'),
('FAC-2025-0011', 210.0, 0.0, 210.0, '2025-12-01 10:15:00', NULL, 'EN_RETARD', 11, 12, 'fact.pdf')

ON DUPLICATE KEY UPDATE
    `montant_ttc` = VALUES(`montant_ttc`),
    `statut` = VALUES(`statut`),
    `date_paiement` = VALUES(`date_paiement`);