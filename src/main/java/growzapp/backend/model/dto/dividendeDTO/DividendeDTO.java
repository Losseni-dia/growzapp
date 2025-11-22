package growzapp.backend.model.dto.dividendeDTO;

import growzapp.backend.model.enumeration.MoyenPaiement;
import growzapp.backend.model.enumeration.StatutDividende;

import java.time.LocalDate;

public record DividendeDTO(
        Long id,
        double montantParPart,
        StatutDividende statutDividende,
        MoyenPaiement moyenPaiement,
        LocalDate datePaiement,
        Long investissementId,
        String investissementInfo,
        double montantTotal,
        String fileName) {
}