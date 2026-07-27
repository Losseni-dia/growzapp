package growzapp.backend.module.dividende.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Un versement de dividende individuel, pour affichage dans le détail d'une position de portefeuille")
public record DividendeSnapshotDTO(
        @Schema(description = "Identifiant du dividende", example = "33") Long id,

        @Schema(description = "Date de paiement effective ou planifiée", example = "2026-03-15") LocalDate datePaiement,

        @Schema(description = "Montant total de ce versement", type = "number", format = "double", example = "37500.00") BigDecimal montantTotal,

        @Schema(description = "Statut du dividende", example = "PAYE") String statutDividende,

        @Schema(description = "Motif ou description du versement", example = "Dividendes T4 2025") String motif) {
}