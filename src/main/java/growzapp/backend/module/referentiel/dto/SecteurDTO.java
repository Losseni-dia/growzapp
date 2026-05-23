package growzapp.backend.module.referentiel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Représentation d'un secteur d'activité")
public record SecteurDTO(
        @Schema(description = "Identifiant unique du secteur", example = "2")
        Long id,

        @Schema(description = "Nom du secteur d'activité", example = "Énergie renouvelable")
        String nom,

        @Schema(description = "Libellés des projets appartenant à ce secteur", example = "[\"Ferme solaire Bobo-Dioulasso\", \"Parc éolien Sahel\"]")
        List<String> projets
) {
}
