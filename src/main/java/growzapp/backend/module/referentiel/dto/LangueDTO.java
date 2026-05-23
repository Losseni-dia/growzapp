package growzapp.backend.module.referentiel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Représentation d'une langue parlée sur la plateforme")
public record LangueDTO(
        @Schema(description = "Identifiant unique de la langue", example = "1")
        Long id,

        @Schema(description = "Nom de la langue", example = "Français")
        String nom
) {
}
