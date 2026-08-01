-- Rattrapage : les projets déjà pleinement financés avant l'introduction du
-- statut FINANCE (V8) basculent une fois pour toutes vers ce statut, comme
-- s'ils venaient de recevoir leur dernier investissement validé.
UPDATE projets
SET statut_projet = 'FINANCE'
WHERE statut_projet = 'VALIDE'
  AND objectif_financement IS NOT NULL
  AND montant_collecte >= objectif_financement;
