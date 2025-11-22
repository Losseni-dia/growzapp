package growzapp.backend.model.enumeration;

public enum StatutPartInvestissement {
    EN_ATTENTE("Investissement en attente de validation"),
    VALIDE("Investissement validé"),
    ANNULE("Investissement annulé"),
    REMBOURSE("Investissement remboursé");

    private String statut;

    StatutPartInvestissement(String statut) {
        this.statut = statut;
    }

    public String getStatut() {
        return statut;
    }
       
}
