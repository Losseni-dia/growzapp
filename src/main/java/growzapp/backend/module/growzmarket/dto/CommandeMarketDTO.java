package growzapp.backend.module.growzmarket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommandeMarketDTO(
        Long id,
        Long projetId,
        String projetLibelle,
        String porteurNom,
        Long acheteurId,
        String acheteurNom,
        BigDecimal montantTotal,
        String statut,
        boolean confirmationLieuRetrait,
        LocalDateTime dateCommande,
        LocalDateTime datePrete,
        LocalDateTime dateRetraitConfirme,
        String motifLitige,
        String factureUrl,
        List<CommandeMarketLigneDTO> lignes) {
}
