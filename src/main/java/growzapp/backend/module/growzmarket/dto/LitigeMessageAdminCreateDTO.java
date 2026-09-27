package growzapp.backend.module.growzmarket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LitigeMessageAdminCreateDTO(
        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 3, max = 1000, message = "Le message doit contenir entre 3 et 1000 caractères")
        String message,

        @NotBlank(message = "Le destinataire est obligatoire")
        @Pattern(regexp = "ACHETEUR|VENDEUR", message = "Le destinataire doit être ACHETEUR ou VENDEUR")
        String destinataire) {
}
