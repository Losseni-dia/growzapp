package growzapp.backend.module.fournisseur.enums;

public enum StatutCommande {
    // Créée par le porteur, en attente de validation admin.
    EN_ATTENTE_VALIDATION,
    // Rejetée par l'admin avant même d'être proposée au fournisseur.
    REJETEE,
    // Validée par l'admin, en attente d'acceptation par le fournisseur —
    // aucun fonds ne bouge encore à ce stade.
    EN_ATTENTE_ACCEPTATION,
    // Le fournisseur refuse (avec motif) — aucun fonds n'a jamais bougé.
    REFUSEE,
    // Le fournisseur accepte de traiter la commande.
    ACCEPTEE,
    // Le fournisseur a expédié — facture obligatoire à cette étape.
    EXPEDIEE,
    // Le porteur conteste la livraison avant confirmation.
    LITIGE,
    // Litige arbitré en faveur du porteur — aucun paiement, aucun fonds
    // n'ayant jamais été débité.
    ANNULEE,
    // Le porteur confirme avoir reçu la commande — déclenche l'alerte de
    // paiement côté admin.
    LIVREE,
    // L'admin a exécuté le paiement : unique mouvement de fonds, wallet
    // projet -> wallet fournisseur.
    PAYEE
}
