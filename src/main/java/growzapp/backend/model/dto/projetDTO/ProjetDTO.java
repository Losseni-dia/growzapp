package growzapp.backend.model.dto.projetDTO;

import growzapp.backend.model.dto.documentDTO.DocumentDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.enumeration.StatutProjet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
                LocalDateTime dateDebut,
                LocalDateTime dateFin,
                double valeurTotalePartsEnPourcent,
                StatutProjet statutProjet,
                LocalDateTime createdAt,
                // --- AJOUT DU CHAMP DE CERTIFICATION ---
                LocalDateTime certifiedAt,
                Long localiteId,
                Long porteurId,
                Long siteId,
                Long secteurId,
                Long paysId,
                String paysNom,
                String localiteNom,
                String porteurNom,
                String siteNom,
                String secteurNom,
                BigDecimal latitude,
                BigDecimal longitude,
                String what3words,
                String googleMapsUrl,
                List<DocumentDTO> documents,
                List<InvestissementDTO> investissements) {

        // === MÉTHODE WITH PERSONNALISÉE POUR LE POSTER ===
        public ProjetDTO withPoster(String newPoster) {
                return new ProjetDTO(
                                id,
                                slug, newPoster, reference, libelle, description, valuation, roiProjete,
                                partsDisponible, partsPrises, prixUnePart, objectifFinancement,
                                montantCollecte, currencyCode, dateDebut, dateFin,
                                valeurTotalePartsEnPourcent, statutProjet, createdAt,
                                certifiedAt, // <--- Ne pas oublier ici
                                localiteId, porteurId, siteId, secteurId, paysId, paysNom, localiteNom,
                                porteurNom, siteNom, secteurNom, latitude, longitude, what3words, googleMapsUrl,
                                documents, investissements);
        }

        // Bonus : avec ID (utile pour update)
        public ProjetDTO withId(Long newId) {
                return new ProjetDTO(
                                newId, slug, poster, reference, libelle, description, valuation, roiProjete,
                                partsDisponible, partsPrises, prixUnePart, objectifFinancement,
                                montantCollecte, currencyCode, dateDebut, dateFin,
                                valeurTotalePartsEnPourcent, statutProjet, createdAt,
                                certifiedAt, // <--- Ne pas oublier ici
                                localiteId, porteurId, siteId, secteurId, paysId, paysNom, localiteNom,
                                porteurNom, siteNom, secteurNom, latitude, longitude, what3words, googleMapsUrl,
                                documents, investissements);
        }
}