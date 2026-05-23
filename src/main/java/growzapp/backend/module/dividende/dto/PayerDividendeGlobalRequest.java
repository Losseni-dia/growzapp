package growzapp.backend.module.dividende.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Corps de la requête pour déclencher le paiement global des dividendes d'un projet")
public record PayerDividendeGlobalRequest(
        @NotNull(message = "Le montant total est obligatoire")
        @Min(value = 1, message = "Le montant doit être supérieur à 0")
        @Schema(description = "Montant total à distribuer entre tous les investisseurs du projet", type = "number", format = "double", example = "5000.00")
        Double montantTotal,

        @NotBlank(message = "Le motif est obligatoire")
        @Schema(description = "Motif du versement affiché sur les factures", example = "Dividendes T4 2025 — Ferme solaire Bobo-Dioulasso")
        String motif,

        @NotBlank(message = "La période est obligatoire (ex: Q4 2025, Décembre 2025...)")
        @Schema(description = "Période couverte par ce versement", example = "Q4 2025")
        String periode
) {
}
