-- V14__add_montant_investi_double.sql

DROP PROCEDURE IF EXISTS AddColumnMontantInvesti;

DELIMITER $$
CREATE PROCEDURE AddColumnMontantInvesti()
BEGIN
    IF NOT EXISTS (
        SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = DATABASE() 
          AND TABLE_NAME = 'investissements' 
          AND COLUMN_NAME = 'montant_investi'
    ) THEN
        ALTER TABLE investissements 
        ADD COLUMN montant_investi DOUBLE NOT NULL DEFAULT 0.0;
    END IF;
END$$
DELIMITER ;

CALL AddColumnMontantInvesti();
DROP PROCEDURE AddColumnMontantInvesti;

-- Mise à jour des anciens investissements
UPDATE investissements i
INNER JOIN projets p ON i.projet_id = p.id
SET i.montant_investi = i.nombre_parts_pris * p.prix_une_part
WHERE i.montant_investi = 0.0 OR i.montant_investi IS NULL;