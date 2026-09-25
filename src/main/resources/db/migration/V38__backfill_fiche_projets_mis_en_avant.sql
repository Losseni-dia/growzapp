-- Backfill : les fiches porteur existantes n'avaient qu'un texte libre
-- (fiche_projets_precedents) figeant les statuts en francais. On relie
-- desormais chaque fiche deja soumise a ses vrais projets (hors brouillon),
-- pour que l'affichage utilise le libelle traduit (DeepL) et le statut
-- traduit (i18n) au lieu du texte fige.
INSERT INTO fiche_projets_mis_en_avant (user_id, projet_id)
SELECT p.user_id, p.id
FROM projets p
JOIN users u ON u.id = p.user_id
WHERE u.fiche_statut <> 'NON_SOUMISE'
  AND p.statut_projet <> 'BROUILLON'
  AND NOT EXISTS (
      SELECT 1 FROM fiche_projets_mis_en_avant f WHERE f.user_id = p.user_id
  );
