package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Un point de vitesse de levée de fonds (agrégat mensuel), sans aucune donnée individuelle d'investisseur")
public record VelocitePointDTO(
        @Schema(description = "Période au format AAAA-MM", example = "2026-03") String periode,

        @Schema(description = "Montant total investi durant cette période", example = "1250000") BigDecimal montant,

        @Schema(description = "Nombre d'investissements réalisés durant cette période", example = "4") int nombreInvestissements) {
}