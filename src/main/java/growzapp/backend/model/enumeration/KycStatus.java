package growzapp.backend.model.enumeration;

public enum KycStatus {
    NON_SOUMIS, // L'utilisateur n'a encore rien envoyé
    EN_ATTENTE, // Documents envoyés, en attente de l'admin
    VALIDE, // Identité vérifiée avec succès
    REJETE // Documents refusés (mauvaise qualité, expiration, etc.)
}