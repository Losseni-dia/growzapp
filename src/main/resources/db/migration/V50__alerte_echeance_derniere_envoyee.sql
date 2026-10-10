-- Evite de renotifier plusieurs fois le meme jour l'alerte d'echeance
-- (J-30 ou depassee) si le job planifie tourne ou est relance plusieurs fois.
ALTER TABLE projets ADD COLUMN derniere_alerte_echeance_envoyee_le DATE;
