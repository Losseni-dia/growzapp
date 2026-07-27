package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "KPIs globaux de la plateforme, pour le dashboard admin")
public record GlobalKpiDTO(
        @Schema(description = "Montant total collecté, tous projets confondus", example = "125000000") BigDecimal totalCollecte,

        @Schema(description = "Objectif de financement total, tous projets confondus", example = "500000000") BigDecimal totalObjectif,

        @Schema(description = "Taux de complétion global en pourcentage", example = "25.0") double tauxCompletionGlobal,

        @Schema(description = "Nombre total d'utilisateurs inscrits") long countUsers,

        @Schema(description = "Nombre d'investisseurs uniques ayant au moins un investissement validé") long countInvestisseursUniques,

        @Schema(description = "Nombre de projets au statut VALIDE") long countProjetsActifs,

        @Schema(description = "Nombre total d'investissements validés") long countInvestissements,

        @Schema(description = "Montant moyen investi par investissement", example = "1250000") BigDecimal montantMoyenInvestissement) {
}