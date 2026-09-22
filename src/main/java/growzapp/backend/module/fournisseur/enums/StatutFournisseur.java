package growzapp.backend.module.fournisseur.enums;

public enum StatutFournisseur {
    // Fiche en cours de rédaction, jamais soumise à l'admin — sauvegardable
    // à tout moment, même incomplète.
    BROUILLON,
    EN_ATTENTE,
    VALIDE,
    REJETE
}
