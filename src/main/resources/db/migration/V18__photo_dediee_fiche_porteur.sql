-- Photo dédiée à la fiche de présentation, distincte de l'avatar de compte
-- du porteur (qu'il gère lui-même) — l'admin peut choisir une photo
-- différente lors de la création/édition de la fiche.
ALTER TABLE users ADD COLUMN fiche_photo_url VARCHAR(255);
