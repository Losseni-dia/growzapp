package growzapp.backend.module.growzmarket.dto;

import java.math.BigDecimal;

public record CommandeMarketLigneDTO(
        Long id,
        Long articleId,
        String libelle,
        BigDecimal prixUnitaire,
        int quantite,
        BigDecimal sousTotal) {
}
