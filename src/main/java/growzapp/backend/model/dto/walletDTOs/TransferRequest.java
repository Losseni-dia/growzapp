// src/main/java/growzapp/backend/model/dto/walletDTOs/TransferRequest.java
package growzapp.backend.model.dto.walletDTOs;

public record TransferRequest(
        Long destinataireUserId,
        double montant,
        String source // "DISPONIBLE" ou "RETIRABLE"
) {
    public TransferRequest {
        if (destinataireUserId == null || destinataireUserId <= 0)
            throw new IllegalArgumentException("Destinataire invalide");
        if (montant <= 0)
            throw new IllegalArgumentException("Montant doit être positif");
        if (source == null || (!source.equals("DISPONIBLE") && !source.equals("RETIRABLE")))
            throw new IllegalArgumentException("Source doit être 'DISPONIBLE' ou 'RETIRABLE'");
    }
}