package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Requête de revalorisation manuelle d'un projet par un administrateur")
public record RevalorisationRequestDTO(
        @Schema(description = "Nouvelle valorisation du projet", example = "28000000", required = true) BigDecimal nouvelleValorisation,

        @Schema(description = "Motif de la revalorisation (ex: audit trimestriel, nouvelle levée...)", example = "Audit Q1 2026 — croissance chiffre d'affaires") String motif) {
}