package growzapp.backend.module.projet.enums;

// growzapp/backend/model/enumeration/StatutProjet.java
public enum StatutProjet {
    BROUILLON,
    EN_PREPARATION,
    SOUMIS,
    VALIDE,
    REJETE,
    EN_COURS,
    TERMINE,
    EN_ATTENTE,
    FINANCE;

    // Projet réellement validé par l'admin et publié (financement en cours
    // ou déjà financé) — à l'exclusion des brouillons, projets en attente
    // de validation, et rejetés. Référence unique pour toute fonctionnalité
    // qui ne doit s'ouvrir qu'aux projets réels : GrowzMarket, commandes
    // fournisseur, mise en avant sur la fiche porteur, etc.
    public boolean estPublie() {
        return this == VALIDE || this == EN_COURS || this == TERMINE || this == FINANCE;
    }
}