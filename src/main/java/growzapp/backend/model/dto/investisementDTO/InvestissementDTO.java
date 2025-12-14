package growzapp.backend.model.dto.investisementDTO;

import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import lombok.Builder;

import java.math.BigDecimal; // Import important
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record InvestissementDTO(
                Long id,
                int nombrePartsPris,
                LocalDateTime date,
                double valeurPartsPrisEnPourcent, // C'est un %, on peut laisser en double
                double frais, // Si c'est un %, double est OK, sinon BigDecimal si montant fixe
                StatutPartInvestissement statutPartInvestissement,

                // Relations
                Long investisseurId,
                String investisseurNom,
                Long projetId,
                String projetLibelle,
                BigDecimal prixUnePart, // CHANGÉ : argent

                // AJOUTÉS POUR LE FRONT
                BigDecimal montantInvesti, // CHANGÉ : argent
                String projetPoster,
                String contratUrl,
                String numeroContrat,

                // Dividendes & ROI
                List<DividendeDTO> dividendes,
                BigDecimal montantTotalPercu, // CHANGÉ : argent
                BigDecimal montantTotalPlanifie, // CHANGÉ : argent
                double roiRealise, // Pourcentage, double OK
                int dividendesPayes,
                int dividendesPlanifies,
                String statutGlobalDividendes) {
}