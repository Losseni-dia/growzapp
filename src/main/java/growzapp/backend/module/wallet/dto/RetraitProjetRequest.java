package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Corps de la requête de retrait depuis le wallet d'un projet vers le porteur")
public record RetraitProjetRequest(
        @NotNull @Positive
        @Schema(description = "Montant à retirer depuis le wallet projet", type = "number", format = "double", example = "500.00")
        BigDecimal montant,

        @NotBlank
        @Schema(description = "Méthode de retrait choisie", example = "MOBILE_MONEY",
                allowableValues = {"MOBILE_MONEY", "STRIPE"})
        String methode,

        @Schema(description = "Numéro de téléphone Mobile Money (obligatoire si methode = MOBILE_MONEY)", example = "+22670123456")
        String phone
) {
}
