package growzapp.backend.module.user.dto;

import growzapp.backend.module.referentiel.dto.LocaliteDTO;
import growzapp.backend.module.user.enums.Sexe;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.projet.dto.ProjetDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@Data
@Schema(description = "Représentation complète d'un utilisateur de la plateforme Growzapp")
public class UserDTO {

    @Schema(description = "Identifiant unique de l'utilisateur", example = "1")
    private Long id;

    @Schema(description = "URL de la photo de profil", example = "/uploads/profiles/john-doe.jpg")
    private String image;

    @Schema(description = "Identifiant de connexion", example = "john.doe")
    private String login;

    @Schema(hidden = true)
    private String password;

    @Schema(description = "Prénom de l'utilisateur", example = "John")
    private String prenom;

    @Schema(description = "Nom de famille de l'utilisateur", example = "Doe")
    private String nom;

    @Schema(description = "Sexe de l'utilisateur", example = "M", allowableValues = {"M", "F", "X"})
    private Sexe sexe;

    @Schema(description = "Adresse email", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Numéro de téléphone", example = "+22670123456")
    private String contact;

    @Schema(description = "Localité de résidence")
    private LocaliteDTO localite;

    @Schema(description = "Code de la langue d'interface préférée", example = "fr")
    private String interfaceLanguage;

    @Schema(description = "Rôles de sécurité attribués à l'utilisateur", example = "[\"ROLE_USER\"]")
    private List<String> roles = new ArrayList<>();

    @Schema(description = "Codes des langues parlées par l'utilisateur", example = "[\"fr\", \"en\"]")
    private List<String> langues = new ArrayList<>();

    @Schema(description = "Investissements réalisés par l'utilisateur")
    private List<InvestissementDTO> investissements = new ArrayList<>();

    @Schema(description = "Projets créés par l'utilisateur")
    private List<ProjetDTO> projets = new ArrayList<>();

    @Schema(description = "Indique si le compte est actif", example = "true")
    private boolean enabled = true;

    // === Champs KYC ===

    @Schema(
        description = "Statut de vérification d'identité (KYC)",
        example = "NON_SOUMIS",
        allowableValues = {"NON_SOUMIS", "EN_ATTENTE", "VALIDE", "REJETE"}
    )
    private KycStatus kycStatus;

    @Schema(description = "Numéro de la pièce d'identité soumise", example = "BF123456789")
    private String kycNumeroPiece;

    @Schema(description = "Date de délivrance de la pièce d'identité", example = "2020-01-15")
    private java.time.LocalDate kycDateDelivrance;

    @Schema(description = "Date d'expiration de la pièce d'identité", example = "2030-01-14")
    private java.time.LocalDate kycDateExpiration;

    @Schema(description = "URL du recto de la pièce d'identité", example = "/uploads/kyc/recto-john.jpg")
    private String kycRectoUrl;

    @Schema(description = "URL du verso de la pièce d'identité", example = "/uploads/kyc/verso-john.jpg")
    private String kycVersoUrl;

    @Schema(description = "URL du selfie de vérification KYC", example = "/uploads/kyc/selfie-john.jpg")
    private String kycSelfieUrl;

    @Schema(description = "Motif de rejet des documents KYC par l'administrateur", example = "Documents illisibles, veuillez soumettre à nouveau.")
    private String kycCommentaireRejet;
}
