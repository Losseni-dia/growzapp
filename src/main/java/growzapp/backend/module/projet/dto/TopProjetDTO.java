package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Un projet dans un classement (par collecte ou par performance)")
public record TopProjetDTO(
                @Schema(description = "Identifiant du projet", example = "14") Long projetId,

                @Schema(description = "Libellé du projet", example = "Ferme solaire Bobo-Dioulasso") String libelle,

                @Schema(description = "Libellé traduit du projet") String libelleTradu,

                @Schema(description = "Poster du projet") String poster,

                @Schema(description = "Montant collecté", example = "8500000") BigDecimal montantCollecte,

                @Schema(description = "Objectif de financement", example = "10000000") BigDecimal objectifFinancement,

                @Schema(description = "Pourcentage de progression de la collecte", example = "85.0") double progressionPourcent) {
}