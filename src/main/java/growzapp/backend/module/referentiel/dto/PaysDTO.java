package growzapp.backend.module.referentiel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Représentation d'un pays")
public record PaysDTO(
        @Schema(description = "Identifiant unique du pays", example = "1")
        Long id,

        @Schema(description = "Nom du pays", example = "Burkina Faso")
        String nom,

        @Schema(description = "Noms des localités appartenant à ce pays", example = "[\"Ouagadougou\", \"Bobo-Dioulasso\"]")
        List<String> localites
) {
}
