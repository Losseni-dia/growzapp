package growzapp.backend.module.contrat.dto;

import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Représentation d'un contrat pour la liste d'administration")
public record ContratAdminDTO(
        @Schema(description = "Identifiant du contrat", example = "12") Long id,
        @Schema(description = "Numéro officiel du contrat", example = "CONTRAT-2025-00015") String numeroContrat,
        @Schema(description = "Date de génération du contrat") LocalDateTime dateGeneration,
        @Schema(description = "Libellé du projet lié", example = "Ferme solaire Bobo-Dioulasso") String projet,
        @Schema(description = "Nom complet de l'investisseur", example = "John Doe") String investisseur,
        @Schema(description = "Email de l'investisseur", example = "john.doe@example.com") String emailInvestisseur,
        @Schema(description = "Téléphone de l'investisseur", example = "+225 07 00 00 00") String telephone,
        @Schema(description = "Montant investi", type = "number", format = "double", example = "2500.00") BigDecimal montantInvesti,
        @Schema(description = "Nombre de parts", example = "5") int nombreParts,
        @Schema(description = "Statut de l'investissement lié", example = "VALIDE") StatutPartInvestissement statutInvestissement
) {
}
