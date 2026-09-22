package growzapp.backend.module.fournisseur.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Article ou service proposé par un fournisseur")
public record ArticleFournisseurCreateDTO(

        @NotBlank(message = "Le nom est obligatoire") @Size(max = 150) @Schema(example = "Sac de ciment 50kg") String nom,

        @Size(max = 1000) String description,

        @NotNull(message = "Le prix est obligatoire") @DecimalMin(value = "0.01", message = "Le prix doit être positif") BigDecimal prix,

        @NotBlank(message = "L'unité est obligatoire") @Schema(example = "sac") String unite,

        boolean disponible) {
}
