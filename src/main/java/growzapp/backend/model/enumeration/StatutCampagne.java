package growzapp.backend.model.enumeration;

public enum StatutCampagne {
    EN_PREPARATION("En préparation"),
    OUVERTE("Campagne ouverte"),
    SUCCES(" Campagne réussie"),
    SUSPENDUE("Campagne suspendue"),
    ECHEC (" Campagne échouée"), 
    CLOTUREE(" Campagne cloturée");

    private String statut;

    StatutCampagne(String statut) {
        this.statut = statut;
    }

    public String getStatut() {
        return statut;
    }

}
