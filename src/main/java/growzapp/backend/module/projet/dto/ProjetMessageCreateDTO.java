package growzapp.backend.module.projet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjetMessageCreateDTO(
        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 1, max = 2000, message = "Le message doit contenir entre 1 et 2000 caractères")
        String contenu) {
}
