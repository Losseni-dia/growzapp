package growzapp.backend.model.dto.walletDTOs;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record RetraitProjetRequest(
        @NotNull @Positive BigDecimal montant,
        @NotBlank String methode, // "MOBILE_MONEY" ou "STRIPE"
        String phone // obligatoire si MOBILE_MONEY
) {
}