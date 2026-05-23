package growzapp.backend.module.user.dto;

import growzapp.backend.module.referentiel.dto.LangueDTO;
import growzapp.backend.module.referentiel.dto.LocaliteDTO;
import growzapp.backend.module.user.enums.Sexe;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "Données requises pour créer un nouveau compte utilisateur (envoyé en multipart sous la clé 'user')")
public class UserCreateDTO {

    @NotBlank(message = "Le login est obligatoire")
    @Size(min = 3, max = 60)
    @Schema(description = "Identifiant unique de connexion", example = "john.doe", minLength = 3, maxLength = 60)
    private String login;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6)
    @Schema(description = "Mot de passe (minimum 6 caractères)", example = "motDePasse123!", format = "password", minLength = 6)
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    @Schema(description = "Confirmation du mot de passe — doit être identique au champ password", example = "motDePasse123!", format = "password")
    private String confirmPassword;

    @NotBlank
    @Schema(description = "Prénom de l'utilisateur", example = "John")
    private String prenom;

    @NotBlank
    @Schema(description = "Nom de famille de l'utilisateur", example = "Doe")
    private String nom;

    @NotNull
    @Schema(description = "Sexe de l'utilisateur", example = "M", allowableValues = {"M", "F", "X"})
    private Sexe sexe;

    @Email
    @NotBlank
    @Schema(description = "Adresse email valide", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Numéro de téléphone de contact", example = "+22670123456")
    private String contact;

    @Schema(description = "Localité de résidence de l'utilisateur")
    private LocaliteDTO localite;

    @Schema(description = "Liste des langues parlées par l'utilisateur")
    private List<LangueDTO> langues = new ArrayList<>();

    @Schema(description = "Photo de profil — géré via le champ multipart 'image', ce champ est ignoré à la création", hidden = true)
    private String image;
}
