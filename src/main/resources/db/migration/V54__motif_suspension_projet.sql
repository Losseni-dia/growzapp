-- Motif de suspension d'un projet en cours (EN_COURS -> SUSPENDU) et motif
-- d'arret definitif (SUSPENDU -> TERMINE), affiches aux investisseurs notifies.
ALTER TABLE projets ADD COLUMN motif_suspension VARCHAR(1000);
