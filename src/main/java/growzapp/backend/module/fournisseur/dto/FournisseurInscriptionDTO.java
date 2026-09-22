package growzapp.backend.module.fournisseur.dto;

import growzapp.backend.module.fournisseur.enums.StatutJuridiqueFournisseur;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Inscription d'un fournisseur sur la plateforme")
public record FournisseurInscriptionDTO(

        @NotNull(message = "Le type de fournisseur est obligatoire") StatutJuridiqueFournisseur statutJuridique,

        @Size(max = 150, message = "La raison sociale ne peut pas dépasser 150 caractères")
        @Schema(example = "SARL Matériaux Plus", description = "Obligatoire si type = ENTREPRISE") String raisonSociale,

        @NotBlank(message = "Le secteur d'activité est obligatoire")
        @Schema(example = "Matériaux de construction") String secteurNom,

        @NotBlank(message = "La ville est obligatoire") @Schema(example = "Abidjan") String ville,

        @NotBlank(message = "Le pays est obligatoire") @Schema(example = "Côte d'Ivoire") String pays,

        @Schema(example = "+225 07 00 00 00 00") String telephone,

        @Schema(example = "contact@materiaux-plus.ci") String email,

        @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
        @Schema(example = "Fournisseur de ciment, fer à béton et matériaux de construction à Abidjan.") String description) {
}
