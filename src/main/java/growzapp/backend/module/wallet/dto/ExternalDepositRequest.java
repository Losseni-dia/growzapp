package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps de la requête de dépôt externe (carte ou Mobile Money)")
public record ExternalDepositRequest(
        @Schema(description = "Montant à déposer", type = "number", format = "double", example = "75.00")
        double montant,

        @Schema(description = "Méthode de paiement choisie", example = "STRIPE_CARD",
                allowableValues = {"STRIPE_CARD", "ORANGE_MONEY", "WAVE", "MTN_MOMO"})
        String method
) {
    public ExternalDepositRequest {
        if (montant <= 0)
            throw new IllegalArgumentException("Le montant doit être positif");
        if (method == null || method.isBlank())
            throw new IllegalArgumentException("Méthode requise");
    }
}
