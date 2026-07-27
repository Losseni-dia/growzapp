package growzapp.backend.module.investissement.dto;

import growzapp.backend.module.dividende.dto.DividendeSnapshotDTO;
import growzapp.backend.module.projet.dto.ValorisationSnapshotDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Une ligne de position dans le portefeuille d'un investisseur (façon relevé de compte-titres)")
public record PortefeuilleLigneDTO(
        @Schema(description = "Identifiant de l'investissement", example = "15") Long investissementId,

        @Schema(description = "Identifiant du projet", example = "7") Long projetId,

        @Schema(description = "Libellé du projet", example = "Ferme solaire Bobo-Dioulasso") String projetLibelle,

        @Schema(description = "Libellé du projet traduit selon la langue demandée") String projetLibelleTradu,

        @Schema(description = "Poster du projet") String projetPoster,

        @Schema(description = "Statut actuel du projet", example = "VALIDE") String statutProjet,

        @Schema(description = "Date de l'investissement") LocalDateTime dateInvestissement,

        @Schema(description = "Nombre de parts détenues", example = "10") int nombrePartsPris,

        @Schema(description = "Pourcentage du projet détenu par cette position", example = "2.5") double pourcentageDetenu,

        @Schema(description = "Montant investi initialement (prix d'entrée)", example = "500000") BigDecimal montantInvesti,

        @Schema(description = "Valorisation actuelle du projet", example = "25000000") BigDecimal valorisationActuelle,

        @Schema(description = "Valeur actuelle estimée de cette position (valorisation × % détenu)", example = "625000") BigDecimal valeurPositionActuelle,

        @Schema(description = "Plus ou moins-value latente en pourcentage", example = "25.0") double performancePourcent,

        @Schema(description = "Total des dividendes déjà perçus sur cette position", example = "37500") BigDecimal dividendesPercus,

               @Schema(description = "Historique de valorisation du projet, pour tracer un graphique d'évolution")
        List<ValorisationSnapshotDTO> historiqueValorisation,

        @Schema(description = "Détail des versements de dividendes reçus sur cette position")
        List<DividendeSnapshotDTO> dividendesDetail
) {
}