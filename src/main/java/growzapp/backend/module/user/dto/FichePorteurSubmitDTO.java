package growzapp.backend.module.user.dto;

import growzapp.backend.module.user.enums.StatutJuridiquePorteur;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Soumission de la fiche de présentation du porteur")
public record FichePorteurSubmitDTO(

        @NotBlank(message = "La bio est obligatoire")
        @Size(min = 30, max = 2000, message = "La bio doit contenir entre 30 et 2000 caractères")
        String bio,

        @NotNull(message = "Le statut (individuel ou société) est obligatoire")
        StatutJuridiquePorteur statutJuridique,

        @Size(max = 150, message = "La raison sociale ne doit pas dépasser 150 caractères")
        String raisonSociale,

        @NotNull(message = "Le nombre d'années d'expérience est obligatoire")
        @Min(value = 0, message = "Le nombre d'années d'expérience ne peut pas être négatif")
        @Max(value = 80, message = "Le nombre d'années d'expérience semble incorrect")
        Integer anneesExperience,

        @Size(max = 2000, message = "La description des projets précédents ne doit pas dépasser 2000 caractères")
        String projetsPrecedents,

        @NotBlank(message = "Le contact téléphonique professionnel est obligatoire")
        String contactTelephone,

        @NotBlank(message = "L'email professionnel est obligatoire")
        String contactEmail,

        String siteWeb,

        String linkedin,

        String reseauxAutres
) {
}
