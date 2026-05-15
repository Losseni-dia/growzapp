package growzapp.backend.module.projet.dto;

import growzapp.backend.model.dto.documentDTO.DocumentDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.enumeration.StatutProjet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Représentation complète d'un projet pour l'affichage")
public record ProjetDTO(
                Long id,
                String slug,
                String poster,
                Integer reference,
                String libelle,
                String description,
                BigDecimal valuation,
                double roiProjete,
                int partsDisponible,
                int partsPrises,
                BigDecimal prixUnePart,
                BigDecimal objectifFinancement,
                BigDecimal montantCollecte,
                String currencyCode,
                LocalDate dateDebut,
                LocalDate dateFin,
                Integer dureeMois,

                @Schema(description = "Pourcentage de l'equity total représenté par les parts") double valeurTotalePartsEnPourcent,

                StatutProjet statutProjet,
                LocalDateTime createdAt,
                LocalDateTime certifiedAt,

                // IDs des relations
                Long localiteId,
                Long porteurId,
                Long siteId,
                Long secteurId,
                Long paysId,

                // Libellés pour éviter des appels API supplémentaires au Front
                String paysNom,
                String localiteNom,
                String porteurNom,
                String siteNom,
                String secteurNom,

                // Données Géo
                BigDecimal latitude,
                BigDecimal longitude,
                String what3words,

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