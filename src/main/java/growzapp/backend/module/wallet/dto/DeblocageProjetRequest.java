package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Corps de la requête de déblocage partiel de trésorerie séquestrée d'un projet")
public record DeblocageProjetRequest(
        @NotNull @Positive
        @Schema(description = "Montant à débloquer, doit être inférieur ou égal au solde bloqué du projet", type = "number", format = "double", example = "200.00")
        BigDecimal montant,

        @Schema(description = "Motif du déblocage (optionnel)", example = "Avance sur travaux — phase 1")
        String motif
) {
}
