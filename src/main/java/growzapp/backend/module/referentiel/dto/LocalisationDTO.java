package growzapp.backend.module.referentiel.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Représentation d'une localisation géographique (site physique d'un projet)")
public record LocalisationDTO(
        @Schema(description = "Identifiant unique de la localisation", example = "3")
        Long id,

        @Schema(description = "Nom du site", example = "Ferme solaire Nord")
        String nom,

        @Schema(description = "Adresse physique complète", example = "Zone Industrielle, Secteur 28, Ouagadougou")
        String adresse,

        @Schema(description = "Numéro de contact du site", example = "+22670123456")
        String contact,

        @Schema(description = "Nom du responsable du site", example = "Amadou Traoré")
        String responsable,

        @Schema(description = "Date et heure de création de l'entrée", example = "2025-06-01T08:00:00")
        LocalDateTime createdAt,

        @Schema(description = "Latitude géographique du site", type = "number", format = "double", example = "12.3647")
        BigDecimal latitude,

        @Schema(description = "Longitude géographique du site", type = "number", format = "double", example = "-1.5328")
        BigDecimal longitude,

        @Schema(description = "Adresse What3Words du site", example = "///lumière.soleil.energie")
        String what3words,

        @Schema(description = "Lien Google Maps vers le site", example = "https://maps.google.com/?q=12.3647,-1.5328")
        String googleMapsUrl,

        @Schema(description = "Nom de la localité de rattachement", example = "Ouagadougou")
        String localiteNom,

        @Schema(description = "Identifiant de la localité de rattachement", example = "1")
        Long localiteId,

        @Schema(description = "Nom du pays", example = "Burkina Faso")
        String paysNom,

        @Schema(description = "Liste des libellés de projets associés à cette localisation", example = "[\"Ferme solaire Bobo-Dioulasso\"]")
        List<String> projets
) {
}
