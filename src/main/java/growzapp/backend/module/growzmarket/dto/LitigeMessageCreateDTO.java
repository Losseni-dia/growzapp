package growzapp.backend.module.growzmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LitigeMessageCreateDTO(
        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 3, max = 1000, message = "Le message doit contenir entre 3 et 1000 caractères")
        String message) {
}
