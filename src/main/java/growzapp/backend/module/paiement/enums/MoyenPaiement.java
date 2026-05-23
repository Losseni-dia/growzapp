package growzapp.backend.module.paiement.enums;

public enum MoyenPaiement {
    VIREMENT("Virement bancaire"),
    CARTE("Bancontact"),
    MOBILE_MONEY("Mobile money"),
    ORANGE_MONEY("Orange money"),
    WAVE("Wave"),                        
    CRYPTO("Crypto"),

    WALLET("Portefeuille growzapp");

    private String moyenPaiement;

    MoyenPaiement(String stamoyenPaiementtut) {
        this.moyenPaiement = stamoyenPaiementtut;
    }

    public String getStatut() {
        return moyenPaiement;
    }
                               
}
