-- La backfill V37 excluait uniquement les brouillons ('BROUILLON'), pas les
-- projets soumis (en attente de validation admin) ni rejetes/en attente -
-- seuls VALIDE/EN_COURS/TERMINE/FINANCE doivent apparaitre comme "projets
-- precedents" sur une fiche porteur.
DELETE FROM fiche_projets_mis_en_avant
WHERE projet_id IN (
    SELECT id FROM projets
    WHERE statut_projet NOT IN ('VALIDE', 'EN_COURS', 'TERMINE', 'FINANCE')
);
