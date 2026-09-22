package growzapp.backend.module.fournisseur.dto;

import jakarta.validation.constraints.NotBlank;

public record MotifDTO(@NotBlank(message = "Le motif est obligatoire") String motif) {
}
