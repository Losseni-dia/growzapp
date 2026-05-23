package growzapp.backend.module.projet.dto;


import growzapp.backend.module.document.dto.DocumentDTO;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Représentation complète d'un projet pour l'affichage")
public record ProjetDTO(
                Long id,

                @Schema(example = "or-blanc-coton-bio", description = "Slug unique pour l'URL SEO") String slug,

                @Schema(example = "/uploads/posters/15_poster.jpg") String poster,

                Integer reference,
                String libelle,
                String description,

                @Schema(type = "number", format = "double", example = "90000.0") BigDecimal valuation,

                @Schema(example = "15.5", description = "ROI projeté en pourcentage") double roiProjete,

                @Schema(example = "300") int partsDisponible,

                @Schema(example = "7") int partsPrises,

                @Schema(type = "number", format = "double", example = "150.0") BigDecimal prixUnePart,

                @Schema(type = "number", format = "double", example = "45000.0") BigDecimal objectifFinancement,

                @Schema(type = "number", format = "double", example = "1050.0") BigDecimal montantCollecte,

                @Schema(example = "XOF") String currencyCode,

                @Schema(example = "2026-01-01") LocalDate dateDebut,

                @Schema(example = "2026-06-01") LocalDate dateFin,

                @Schema(example = "36", defaultValue = "36") Integer dureeMois,

                @Schema(description = "Pourcentage de l'equity total représenté par les parts", example = "50.0") double valeurTotalePartsEnPourcent,

                @Schema(example = "VALIDE") StatutProjet statutProjet,

                LocalDateTime createdAt,
                LocalDateTime certifiedAt,

                // IDs des relations
                Long localiteId,
                Long porteurId,
                Long siteId,
                Long secteurId,
                Long paysId,

                // Libellés (Flattening pour le Front)
                @Schema(example = "Côte d'Ivoire") String paysNom,
                @Schema(example = "Abidjan") String localiteNom,
                @Schema(example = "Losseni Dia") String porteurNom,
                @Schema(example = "Centrale Coton Bio Sud") String siteNom,
                @Schema(example = "Agro-Industrie") String secteurNom,

                // Données Géo
                @Schema(example = "5.3484") BigDecimal latitude,
                @Schema(example = "-4.0305") BigDecimal longitude,
                @Schema(example = "maïs.fonds.récolte") String what3words,

                @Schema(example = "https://maps.google.com/?q=5.34,-4.02") String googleMapsUrl,

                List<DocumentDTO> documents,
                List<InvestissementDTO> investissements) {

        public ProjetDTO withPoster(String newPoster) {
                return new ProjetDTO(id, slug, newPoster, reference, libelle, description, valuation, roiProjete,
                                partsDisponible, partsPrises, prixUnePart, objectifFinancement, montantCollecte,
                                currencyCode,
                                dateDebut, dateFin, dureeMois, valeurTotalePartsEnPourcent, statutProjet, createdAt,
                                certifiedAt, localiteId, porteurId, siteId, secteurId, paysId, paysNom, localiteNom,
                                porteurNom, siteNom, secteurNom, latitude, longitude, what3words, googleMapsUrl,
                                documents, investissements);
        }

        public ProjetDTO withId(Long newId) {
                return new ProjetDTO(newId, slug, poster, reference, libelle, description, valuation, roiProjete,
                                partsDisponible, partsPrises, prixUnePart, objectifFinancement, montantCollecte,
                                currencyCode,
                                dateDebut, dateFin, dureeMois, valeurTotalePartsEnPourcent, statutProjet, createdAt,
                                certifiedAt, localiteId, porteurId, siteId, secteurId, paysId, paysNom, localiteNom,
                                porteurNom, siteNom, secteurNom, latitude, longitude, what3words, googleMapsUrl,
                                documents, investissements);
        }
}