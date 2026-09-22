package growzapp.backend.module.fournisseur.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CommandeDTO(
        Long id,
        Long projetId,
        String projetLibelle,
        Long fournisseurId,
        String fournisseurNom,
        BigDecimal montantTotal,
        String statut,
        LocalDateTime dateCommande,
        LocalDateTime dateValidationAdmin,
        LocalDateTime dateLivraison,
        LocalDateTime dateConfirmationReception,
        String motifRejet,
        String motifLitige,
        List<CommandeLigneDTO> lignes) {
}
