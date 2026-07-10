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
    @Size(min = 3, max = 60, message = "Le login doit contenir entre 3 et 60 caractères")
    @Schema(description = "Identifiant unique de connexion", example = "losseni", minLength = 3, maxLength = 60)
    private String login;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    @Schema(description = "Mot de passe (minimum 6 caractères)", example = "motDePasse123!", format = "password", minLength = 6)
    private String password;

    @NotBlank(message = "La confirmation du mot de passe est obligatoire")
    @Schema(description = "Confirmation du mot de passe — doit être identique au champ password", example = "motDePasse123!", format = "password")
    private String confirmPassword;

    @NotBlank(message = "Le prénom est obligatoire")
    @Schema(description = "Prénom de l'utilisateur", example = "Losseni")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    @Schema(description = "Nom de famille de l'utilisateur", example = "Dia")
    private String nom;

    @NotNull(message = "Le sexe est obligatoire")
    @Schema(description = "Sexe de l'utilisateur", example = "M", allowableValues = { "M", "F", "X" })
    private Sexe sexe;

    @Email(message = "L'adresse email n'est pas valide")
    @Schema(description = "Adresse email (optionnelle — certains utilisateurs africains n'en ont pas)", example = "losseni.dia@example.com")
    private String email;

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^\\+?[0-9\\s\\-]{8,20}$", message = "Numéro de téléphone invalide (ex: +22670123456)")
    @Schema(description = "Numéro de téléphone de contact — obligatoire", example = "+22670123456")
    private String contact;

    @Schema(description = "Localité de résidence de l'utilisateur")
    private LocaliteDTO localite;

    @Schema(description = "Liste des langues parlées par l'utilisateur")
    private List<LangueDTO> langues = new ArrayList<>();

    @Schema(description = "Photo de profil — géré via le champ multipart 'image', ce champ est ignoré à la création", hidden = true)
    private String image;

    @Pattern(regexp = "^(fr|en|es)$", message = "La langue doit être fr, en ou es")
    @Schema(description = "Langue préférée de l'interface", example = "fr", allowableValues = { "fr", "en", "es" })
    private String interfaceLanguage = "fr";

    @Pattern(regexp = "^(XOF|XAF|USD|EUR|GBP|GNF|MAD|NGN|GHS|KES)$", message = "Devise non supportée")
    @Schema(description = "Devise préférée", example = "XOF", allowableValues = { "XOF", "XAF", "USD", "EUR", "GBP",
            "GNF", "MAD", "NGN", "GHS", "KES" })
    private String devisePreferee = "XOF";
}