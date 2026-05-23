package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import growzapp.backend.module.wallet.enums.TypeTransaction;

@Schema(description = "Corps de la requête de retrait externe (Stripe ou Mobile Money)")
public record ExternalWithdrawRequest(
        @Schema(description = "Montant à retirer vers le compte externe", type = "number", format = "double", example = "50.00")
        double montant,

        @Schema(description = "Numéro de téléphone Mobile Money (obligatoire pour ORANGE_MONEY, WAVE, MTN_MOMO)", example = "+22670123456")
        String phone,

        @Schema(description = "Méthode de retrait externe", example = "ORANGE_MONEY",
                allowableValues = {"ORANGE_MONEY", "WAVE", "MTN_MOMO", "BANK_TRANSFER"})
        String method,

        @Schema(description = "Type de transaction correspondant à la méthode choisie", example = "PAYOUT_OM",
                allowableValues = {"PAYOUT_OM", "PAYOUT_WAVE", "PAYOUT_MTN", "PAYOUT_STRIPE", "PAYOUT_BANK"})
        TypeTransaction type
) {
    public ExternalWithdrawRequest {
        if (montant <= 0)
            throw new IllegalArgumentException("Le montant doit être positif");
        if (method == null || method.isBlank())
            throw new IllegalArgumentException("Méthode requise");
        if (List.of("ORANGE_MONEY", "WAVE", "MTN_MOMO").contains(method) && (phone == null || phone.isBlank())) {
            throw new IllegalArgumentException("Numéro de téléphone requis");
        }
    }
}
