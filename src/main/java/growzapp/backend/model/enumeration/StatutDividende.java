package growzapp.backend.model.enumeration;

public enum StatutDividende {
    PLANIFIE("Paiement de dividendes planifié"),
    PAYE("Paiement de dividendes effectué"),
    ANNULE("Paiement de dividendes annulé");

    private String statut;

    StatutDividende(String statut) {
        this.statut = statut;
    }

    public String getStatut() {
        return statut;
    }
    
                     
}
