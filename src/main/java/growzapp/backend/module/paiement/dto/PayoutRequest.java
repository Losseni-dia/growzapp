package growzapp.backend.module.paiement.dto;

import java.math.BigDecimal;

import growzapp.backend.module.wallet.enums.TypeTransaction;

public record PayoutRequest(
        BigDecimal montant,
        String phone,
        TypeTransaction type // PAYOUT_OM, PAYOUT_MTN, PAYOUT_WAVE
) {
}