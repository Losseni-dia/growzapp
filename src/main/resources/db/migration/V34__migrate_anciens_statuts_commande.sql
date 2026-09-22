-- Les commandes créées avant la refonte du cycle de vie (V32) portent encore
-- d'anciens statuts qui n'existent plus dans l'enum StatutCommande, ce qui
-- fait planter Hibernate à la lecture (EnumJavaType.fromName). On les
-- remappe vers l'équivalent le plus proche du nouveau cycle :
--   VALIDEE   (validée admin, fonds séquestrés)      -> EN_ATTENTE_ACCEPTATION
--   LIVREE    (fournisseur déclare avoir expédié)    -> EXPEDIEE
--   CONFIRMEE (porteur confirme, fonds libérés)      -> LIVREE
-- REJETEE, EN_ATTENTE_VALIDATION et LITIGE existaient déjà à l'identique.
UPDATE commandes SET statut = 'EXPEDIEE' WHERE statut = 'LIVREE';
UPDATE commandes SET statut = 'LIVREE' WHERE statut = 'CONFIRMEE';
UPDATE commandes SET statut = 'EN_ATTENTE_ACCEPTATION' WHERE statut = 'VALIDEE';
