package growzapp.backend.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Résultat simplifié d'une recherche d'utilisateur (auto-complétion)")
public class UserSearchDTO {

    @Schema(description = "Identifiant unique de l'utilisateur", example = "42")
    private Long id;

    @Schema(description = "Nom complet affiché (prénom + nom)", example = "John Doe")
    private String nomComplet;

    @Schema(description = "Identifiant de connexion", example = "john.doe")
    private String login;

    @Schema(description = "URL de la photo de profil", example = "/uploads/profiles/john-doe.jpg")
    private String image;
}
