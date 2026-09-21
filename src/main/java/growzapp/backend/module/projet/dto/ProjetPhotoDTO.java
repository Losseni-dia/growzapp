package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Photo additionnelle d'un projet, consultable dans la galerie de la page détail")
public record ProjetPhotoDTO(
                @Schema(description = "Identifiant de la photo", example = "12") Long id,
                @Schema(description = "URL de la photo", example = "/uploads/projet-photos/7_1234567890_chantier.jpg") String url) {
}
