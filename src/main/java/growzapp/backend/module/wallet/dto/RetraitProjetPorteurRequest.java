package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Corps de la requête de retrait Mobile Money d'un porteur depuis le wallet de son projet")
public record RetraitProjetPorteurRequest(
        @NotNull @Positive
        @Schema(description = "Montant à retirer, doit être inférieur ou égal au soldeDisponible du wallet projet", type = "number", format = "double", example = "200.00")
        BigDecimal montant,

        @NotBlank
        @Schema(description = "Numéro Mobile Money destinataire", example = "+22670123456")
        String phone,

        @Schema(description = "Clé d'idempotence générée côté client (évite le double-traitement en cas de double-clic ou de retry réseau)", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String idempotencyKey
) {
}
