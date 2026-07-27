package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Schema(description = "Tableau de bord global admin — vue plateforme complète : investissements, projets, dividendes")
public record AdminGlobalDashboardDTO(
        @Schema(description = "KPIs globaux") GlobalKpiDTO kpis,

        @Schema(description = "Évolution mensuelle du volume d'investissement (montant + nombre)") List<VelocitePointDTO> evolutionInvestissements,

        @Schema(description = "Répartition des projets par secteur (nom → nombre de projets)") Map<String, Long> repartitionParSecteur,

        @Schema(description = "Répartition des projets par statut (statut → nombre de projets)") Map<String, Long> repartitionParStatut,

        @Schema(description = "Top 5 des projets par montant collecté") List<TopProjetDTO> topProjetsParCollecte,

        @Schema(description = "Top 5 des projets par pourcentage de progression") List<TopProjetDTO> topProjetsParProgression,

        @Schema(description = "Montant total des dividendes versés, toute la plateforme") BigDecimal totalDividendesVerses,

        @Schema(description = "Nombre total de versements de dividendes effectués") long countDividendesVerses,

        @Schema(description = "Évolution mensuelle des dividendes versés (montant + nombre de versements)") List<VelocitePointDTO> evolutionDividendes) {
}