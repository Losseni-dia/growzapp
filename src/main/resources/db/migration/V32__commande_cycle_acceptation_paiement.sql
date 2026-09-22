-- Nouveau cycle de commande : validation admin -> acceptation/refus
-- fournisseur -> expédition (facture) -> confirmation réception porteur ->
-- paiement admin (un seul mouvement de fonds, plus de séquestre en deux
-- temps). date_livraison représentait jusqu'ici tantôt l'expédition tantôt
-- la livraison confirmée (ambigu) — renommée en date_expedition, la
-- confirmation de réception utilise déjà sa propre colonne dédiée.
ALTER TABLE commandes RENAME COLUMN date_livraison TO date_expedition;

ALTER TABLE commandes ADD COLUMN date_acceptation TIMESTAMP;
ALTER TABLE commandes ADD COLUMN date_paiement TIMESTAMP;
ALTER TABLE commandes ADD COLUMN motif_refus VARCHAR(500);

-- Suivi de stock optionnel par article — NULL = illimité (comportement
-- historique conservé pour les articles déjà créés).
ALTER TABLE articles_fournisseur ADD COLUMN stock INTEGER;
