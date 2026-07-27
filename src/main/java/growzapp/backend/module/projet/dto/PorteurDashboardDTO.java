package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Tableau de bord global d'un porteur de projet — vue d'ensemble de tous ses projets")
public record PorteurDashboardDTO(
        @Schema(description = "Nombre de projets portés") int nombreProjets,

        @Schema(description = "Montant total collecté sur tous les projets", example = "18500000") BigDecimal totalCollecteTousProjets,

        @Schema(description = "Nombre total d'investisseurs, tous projets confondus (somme, agrégat uniquement)") int totalInvestisseursTousProjets,

        @Schema(description = "Total des dividendes versés sur tous les projets", example = "980000") BigDecimal totalDividendesVersesTousProjets,

        @Schema(description = "Détail par projet") List<PorteurProjetLigneDTO> projets) {
}