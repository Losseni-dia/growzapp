package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Corps de la requête de dépôt (minimum 10,00 €, maximum 2 décimales)")
public record DepotRequest(
        @Schema(description = "Montant à déposer", type = "number", format = "double", example = "150.00")
        BigDecimal montant
) {
    public DepotRequest {
        if (montant == null) { throw new IllegalArgumentException("Le montant est requis"); }
        if (montant.compareTo(BigDecimal.ZERO) <= 0) { throw new IllegalArgumentException("Le montant doit être strictement positif"); }
        if (montant.scale() > 2) { throw new IllegalArgumentException("Maximum 2 décimales autorisées"); }
        if (montant.compareTo(new BigDecimal("10.00")) < 0) { throw new IllegalArgumentException("Le dépôt minimum est de 10,00 €"); }
    }
}
