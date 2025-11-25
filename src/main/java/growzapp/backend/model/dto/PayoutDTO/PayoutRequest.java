package growzapp.backend.model.dto.PayoutDTO;

import growzapp.backend.model.enumeration.TypeTransaction;

import java.math.BigDecimal;

public record PayoutRequest(
        BigDecimal montant,
        String phone,
        TypeTransaction type // PAYOUT_OM, PAYOUT_MTN, PAYOUT_WAVE
) {
}