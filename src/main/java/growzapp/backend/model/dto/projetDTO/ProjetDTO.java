// src/main/java/growzapp/backend/model/dto/projetDTO/ProjetDTO.java
package growzapp.backend.model.dto.projetDTO;

import growzapp.backend.model.dto.documentDTO.DocumentDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.enumeration.StatutProjet;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


public record ProjetDTO(
       Long id,
        String poster,
        Integer reference,
        String libelle,
        String description,
        BigDecimal valuation, // Changé double -> BigDecimal
        double roiProjete,
        int partsDisponible,
        int partsPrises,
        BigDecimal prixUnePart, // Changé double -> BigDecimal
        BigDecimal objectifFinancement, // Changé double -> BigDecimal
        BigDecimal montantCollecte, // Changé double -> BigDecimal
        String currencyCode,
        LocalDateTime dateDebut,
        LocalDateTime dateFin,
        double valeurTotalePartsEnPourcent,
        StatutProjet statutProjet,
        LocalDateTime createdAt,
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
        List<DocumentDTO> documents,
        List<InvestissementDTO> investissements

        // AJOUTÉ : SOLDE RÉEL DU WALLET (SEULEMENT ADMIN)
         ) 
 {

    // === MÉTHODE WITH PERSONNALISÉE POUR LE POSTER ===
    public ProjetDTO withPoster(String newPoster) {
            return new ProjetDTO(
                            id, newPoster, reference, libelle, description, valuation, roiProjete,
                            partsDisponible, partsPrises, prixUnePart, objectifFinancement,
                            montantCollecte, currencyCode, dateDebut, dateFin,
                            valeurTotalePartsEnPourcent, statutProjet, createdAt, localiteId,
                            porteurId, siteId, secteurId, paysId, paysNom, localiteNom,
                            porteurNom, siteNom, secteurNom, documents, investissements);
    }

    // Bonus : avec ID (utile pour update)
    public ProjetDTO withId(Long newId) {
            return new ProjetDTO(
                            newId, poster, reference, libelle, description, valuation, roiProjete,
                            partsDisponible, partsPrises, prixUnePart, objectifFinancement,
                            montantCollecte, currencyCode, dateDebut, dateFin,
                            valeurTotalePartsEnPourcent, statutProjet, createdAt, localiteId,
                            porteurId, siteId, secteurId, paysId, paysNom, localiteNom,
                            porteurNom, siteNom, secteurNom, documents, investissements);
    }
}