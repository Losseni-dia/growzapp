package growzapp.backend.module.growzmarket.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CommandeMarketLigneCreateDTO(
        @NotNull Long articleId,
        @Min(1) int quantite) {
}
