package growzapp.backend.model.dto.walletDTOs;

public record DepotRequest(double montant) {
    public DepotRequest{if(montant<=0){throw new IllegalArgumentException("Le montant du dépôt doit être positif");}}
}