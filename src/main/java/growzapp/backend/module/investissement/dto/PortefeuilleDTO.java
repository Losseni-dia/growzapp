package growzapp.backend.module.investissement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Portefeuille complet d'un investisseur — vue synthétique de toutes ses positions")
public record PortefeuilleDTO(
        @Schema(description = "Montant total investi (capital engagé)", example = "2500000") BigDecimal totalInvesti,

        @Schema(description = "Valeur actuelle estimée du portefeuille (somme des positions revalorisées)", example = "3125000") BigDecimal valeurActuelleTotale,

        @Schema(description = "Total des dividendes perçus toutes positions confondues", example = "187500") BigDecimal totalDividendesPercus,

        @Schema(description = "Performance globale du portefeuille en pourcentage", example = "25.0") double performanceGlobalePourcent,

        @Schema(description = "Nombre de positions actives") int nombrePositions,

        @Schema(description = "Détail de chaque position") List<PortefeuilleLigneDTO> lignes) {
}