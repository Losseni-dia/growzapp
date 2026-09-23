package growzapp.backend.module.growzmarket.dto;

import java.math.BigDecimal;
import java.util.List;

public record ArticleMarketDTO(
        Long id,
        Long projetId,
        String projetLibelle,
        String porteurNom,
        String nom,
        String description,
        BigDecimal prix,
        String unite,
        boolean disponible,
        Integer stock,
        String categorie,
        String delaiPreparation,
        List<String> photos,
        String pointRetrait,
        String telephoneContact) {
}
