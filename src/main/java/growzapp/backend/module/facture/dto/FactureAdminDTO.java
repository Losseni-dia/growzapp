package growzapp.backend.module.facture.dto;

import growzapp.backend.module.facture.enums.StatutFacture;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Représentation d'une facture pour la liste d'administration")
public record FactureAdminDTO(
        @Schema(example = "22") Long id,
        @Schema(example = "FAC-2025-000022") String numeroFacture,
        @Schema(example = "375.00") double montantHT,
        @Schema(example = "375.00") double montantTTC,
        StatutFacture statut,
        LocalDateTime dateEmission,
        LocalDateTime datePaiement,
        @Schema(description = "Nom complet de l'investisseur", example = "John Doe") String investisseurNom,
        @Schema(description = "Email de l'investisseur", example = "john.doe@example.com") String investisseurEmail
) {
}
