package growzapp.backend.module.fournisseur.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CommandeLigneCreateDTO(
        @NotNull(message = "L'article est obligatoire") Long articleId,
        @Min(value = 1, message = "La quantité doit être d'au moins 1") int quantite) {
}
