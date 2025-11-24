package growzapp.backend.model.dto.walletDTOs;

public record RetraitRequest(double montant) {
    public RetraitRequest{if(montant<=0)throw new IllegalArgumentException("Le montant doit être positif");}
}