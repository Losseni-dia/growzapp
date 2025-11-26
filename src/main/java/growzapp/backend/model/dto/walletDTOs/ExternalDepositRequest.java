// src/main/java/growzapp/backend/model/dto/walletDTOs/ExternalDepositRequest.java
package growzapp.backend.model.dto.walletDTOs;

public record ExternalDepositRequest(
        double montant,
        String method // "STRIPE_CARD", "ORANGE_MONEY", "WAVE", "MTN_MOMO"
) {
    public ExternalDepositRequest {
        if (montant <= 0)
            throw new IllegalArgumentException("Le montant doit être positif");
        if (method == null || method.isBlank())
            throw new IllegalArgumentException("Méthode requise");
    }
}