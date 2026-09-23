package growzapp.backend.module.growzmarket.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ArticleMarketCreateDTO(
        @NotNull Long projetId,
        @NotBlank String nom,
        String description,
        @NotNull @Positive BigDecimal prix,
        @NotBlank String unite,
        boolean disponible,
        Integer stock,
        String categorie,
        String delaiPreparation,
        @NotBlank String pointRetrait,
        String telephoneContact) {
}
