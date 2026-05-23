package growzapp.backend.module.user.dto;

import growzapp.backend.module.referentiel.dto.LangueDTO;
import growzapp.backend.module.referentiel.dto.LocaliteDTO;
import growzapp.backend.module.user.enums.Sexe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Données de mise à jour du profil utilisateur (envoyé en multipart sous la clé 'user')")
public class UserUpdateDTO {

    @Schema(description = "Identifiant interne de l'utilisateur", example = "1")
    private Long id;

    @Schema(description = "Photo de profil en base64 — géré via le champ multipart 'image'", hidden = true)
    private String image;

    @Size(min = 3, max = 60)
    @Schema(description = "Nouvel identifiant de connexion", example = "john.doe.updated", minLength = 3, maxLength = 60)
    private String login;

    @Schema(description = "Nouveau mot de passe (laisser vide pour conserver l'actuel)", example = "NouveauMdp456!", format = "password")
    private String password;

    @Schema(description = "Prénom", example = "John")
    private String prenom;

    @Schema(description = "Nom de famille", example = "Doe")
    private String nom;

    @Schema(description = "Sexe", example = "M", allowableValues = {"M", "F", "X"})
    private Sexe sexe;

    @Email
    @Schema(description = "Adresse email", example = "john.updated@example.com")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Numéro de téléphone invalide")
    @Schema(description = "Numéro de téléphone au format international", example = "+22670123456")
    private String contact;

    @Schema(description = "Nouvelle localité de résidence")
    private LocaliteDTO localite;

    @Schema(description = "Nouvelles langues parlées (remplace intégralement les anciennes)")
    private List<LangueDTO> langues = new ArrayList<>();
}
