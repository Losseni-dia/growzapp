package growzapp.backend.model.dto.dividendeDTO;

import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.enumeration.MoyenPaiement;
import growzapp.backend.model.enumeration.StatutDividende;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DividendeDTO(
        Long id,
        BigDecimal montantParPart,
        StatutDividende statutDividende,
        MoyenPaiement moyenPaiement,
        LocalDate datePaiement,
        Long investissementId,
        String investissementInfo,
        BigDecimal montantTotal,
        String fileName, // optionnel, pour compatibilité
        String factureUrl, // le plus important : URL directe du PDF
        FactureDTO facture,
        String motif
) {
}