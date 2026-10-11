-- Motif de rejet d'un projet, renseigne par l'admin -- affiche au porteur
-- pour qu'il sache quoi corriger avant de rouvrir le projet pour modification.
ALTER TABLE projets ADD COLUMN motif_rejet VARCHAR(1000);
