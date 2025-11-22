package growzapp.backend.model.enumeration;

// growzapp/backend/model/enumeration/StatutProjet.java
public enum StatutProjet {
    EN_PREPARATION,
    SOUMIS,
    VALIDE,
    REJETE,
    EN_COURS,
    TERMINE,
    EN_ATTENTE;

    // Optionnel : si tu veux un libellé lisible
    public String getLibelle() {
        return switch (this) {
            case EN_PREPARATION -> "En préparation";
            case SOUMIS -> "En attente de validation";
            case VALIDE -> "Validé & publié";
            case REJETE -> "Rejeté";
            case EN_COURS -> "En cours de financement";
            case TERMINE -> "Terminé";
            case EN_ATTENTE -> "En attente";
        };
    }
}