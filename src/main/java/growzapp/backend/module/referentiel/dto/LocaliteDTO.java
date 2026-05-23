package growzapp.backend.module.referentiel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Représentation d'une localité (ville ou commune)")
public record LocaliteDTO(
        @Schema(description = "Identifiant unique de la localité", example = "1")
        Long id,

        @Schema(description = "Code postal de la localité", example = "01 BP 514")
        String codePostal,

        @Schema(description = "Nom de la localité", example = "Ouagadougou")
        String nom,

        @Schema(description = "Nom du pays auquel appartient la localité", example = "Burkina Faso")
        String paysNom,

        @Schema(description = "Utilisateurs résidant dans cette localité")
        List<UserInfo> users,

        @Schema(description = "Sites (localisations) rattachés à cette localité")
        List<SiteInfo> localisations
) {
    @Schema(description = "Informations résumées d'un utilisateur lié à la localité")
    public record UserInfo(
            @Schema(description = "Nom de l'utilisateur", example = "Doe")
            String nom,

            @Schema(description = "Prénom de l'utilisateur", example = "John")
            String prenom,

            @Schema(description = "Email de l'utilisateur", example = "john.doe@example.com")
            String email
    ) {
    }

    @Schema(description = "Informations résumées d'un site rattaché à la localité")
    public record SiteInfo(
            @Schema(description = "Identifiant du site", example = "3")
            Long id,

            @Schema(description = "Nom du site", example = "Ferme solaire Nord")
            String nom,

            @Schema(description = "Contact du site", example = "+22670123456")
            String contact
    ) {
    }
}
