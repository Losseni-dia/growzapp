package growzapp.backend.module.fournisseur.dto;

import java.math.BigDecimal;

public record ArticleFournisseurDTO(
        Long id,
        Long fournisseurId,
        String nom,
        String description,
        BigDecimal prix,
        String unite,
        boolean disponible,
        String photoUrl) {
}
