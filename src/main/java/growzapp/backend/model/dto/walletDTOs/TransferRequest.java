package growzapp.backend.model.dto.walletDTOs;


public record TransferRequest(Long destinataireUserId, double montant) {
    public TransferRequest{if(destinataireUserId==null||destinataireUserId<=0)throw new IllegalArgumentException("L'ID du destinataire est requis et doit être valide");if(montant<=0)throw new IllegalArgumentException("Le montant doit être positif");}
}