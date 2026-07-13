package growzapp.backend.module.projet.dto;

import growzapp.backend.module.projet.enums.TypeEvenementValorisation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Un point d'historique de valorisation d'un projet")
public record ValorisationSnapshotDTO(
        @Schema(description = "Date et heure du snapshot", example = "2026-03-15T10:30:00") LocalDateTime date,

        @Schema(description = "Valorisation du projet à cette date", example = "25000000") BigDecimal montantValorisation,

        @Schema(description = "Montant collecté à cette date", example = "12500000") BigDecimal montantCollecte,

        @Schema(description = "Type d'événement ayant déclenché ce snapshot", example = "INVESTISSEMENT") TypeEvenementValorisation typeEvenement,

        @Schema(description = "Montant lié à l'événement (montant investi ou dividende), le cas échéant", example = "500000") BigDecimal montantEvenement) {
}