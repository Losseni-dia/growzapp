package growzapp.backend.module.fournisseur.enums;

public enum StatutCommande {
    // Créée par le porteur, en attente de validation admin — les fonds ne
    // bougent pas encore du wallet projet à ce stade.
    EN_ATTENTE_VALIDATION,
    // Validée par l'admin : fonds débités du wallet projet et placés en
    // séquestre (soldeBloque) sur le wallet du fournisseur.
    VALIDEE,
    REJETEE,
    // Le fournisseur déclare avoir livré — les fonds restent séquestrés
    // jusqu'à confirmation du porteur.
    LIVREE,
    // Le porteur confirme la réception : les fonds séquestrés deviennent
    // disponibles (retirables) pour le fournisseur.
    CONFIRMEE,
    // Litige ouvert par le porteur avant confirmation — l'admin arbitre.
    LITIGE
}
