// src/main/java/growzapp/backend/model/dto/walletDTOs/ExternalWithdrawRequest.java
package growzapp.backend.model.dto.walletDTOs;

import java.util.List;

import growzapp.backend.model.enumeration.TypeTransaction;

public record ExternalWithdrawRequest(
        double montant,
        String phone, // requis pour Mobile Money
        String method, // "ORANGE_MONEY", "WAVE", "MTN_MOMO", "BANK_TRANSFER"
        TypeTransaction type // PAYOUT_OM, PAYOUT_WAVE, etc. (pour compatibilité)
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