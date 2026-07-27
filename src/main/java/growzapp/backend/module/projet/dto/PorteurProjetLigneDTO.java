package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Statistiques d'un projet pour son porteur — jamais d'identité d'investisseur, uniquement des agrégats")
public record PorteurProjetLigneDTO(
        @Schema(description = "Identifiant du projet", example = "14") Long projetId,

        @Schema(description = "Libellé du projet", example = "Ferme solaire Bobo-Dioulasso") String projetLibelle,

        @Schema(description = "Libellé traduit du projet") String projetLibelleTradu,

        @Schema(description = "Poster du projet") String projetPoster,

        @Schema(description = "Statut actuel du projet", example = "VALIDE") String statutProjet,

        @Schema(description = "Objectif de financement", example = "10000000") BigDecimal objectifFinancement,

        @Schema(description = "Montant collecté à ce jour", example = "6500000") BigDecimal montantCollecte,

        @Schema(description = "Pourcentage de l'objectif atteint", example = "65.0") double progressionPourcent,

        @Schema(description = "Nombre d'investisseurs distincts (agrégat uniquement, aucune identité)", example = "23") int nombreInvestisseurs,

        @Schema(description = "Montant moyen investi par investisseur", example = "282608") BigDecimal montantMoyenParInvestisseur,

        @Schema(description = "Solde disponible du wallet projet (prêt à être versé)", example = "1200000") BigDecimal soldeDisponibleWallet,

        @Schema(description = "Solde bloqué du wallet projet (investissements en attente de validation)", example = "300000") BigDecimal soldeBloqueWallet,

        @Schema(description = "Total des dividendes déjà versés aux investisseurs de ce projet", example = "450000") BigDecimal totalDividendesVerses,

        @Schema(description = "Historique de collecte du projet (pour tracer une courbe d'évolution)") List<ValorisationSnapshotDTO> historiqueCollecte,

        @Schema(description = "Vitesse de levée de fonds par mois (agrégats, aucune identité)") List<VelocitePointDTO> vitesseLevee) {
}