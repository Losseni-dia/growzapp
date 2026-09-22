package growzapp.backend.module.fournisseur.dto;

import java.math.BigDecimal;

public record CommandeLigneDTO(
        Long id,
        Long articleId,
        String libelle,
        BigDecimal prixUnitaire,
        int quantite,
        BigDecimal sousTotal) {
}
