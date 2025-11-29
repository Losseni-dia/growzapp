// src/main/java/growzapp/backend/model/dto/investisementDTO/InvestissementDTO.java
package growzapp.backend.model.dto.investisementDTO;

import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record InvestissementDTO(
        Long id,
        int nombrePartsPris,
        LocalDateTime date,
        double valeurPartsPrisEnPourcent,
        double frais,
        StatutPartInvestissement statutPartInvestissement,

        // Relations
        Long investisseurId,
        String investisseurNom,
        Long projetId,
        String projetLibelle,
        double prixUnePart,

        // AJOUTÉS POUR LE FRONT – INDISPENSABLES
        double montantInvesti, // nombrePartsPris × prixUnePart → CALCULÉ CÔTÉ BACK
        String projetPoster,
        String contratUrl,

        // Dividendes & ROI
        List<DividendeDTO> dividendes,
        double montantTotalPercu,
        double montantTotalPlanifie,
        double roiRealise,
        int dividendesPayes,
        int dividendesPlanifies,
        String statutGlobalDividendes) {
}