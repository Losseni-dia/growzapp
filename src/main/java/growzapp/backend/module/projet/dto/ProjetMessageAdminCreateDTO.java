package growzapp.backend.module.projet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProjetMessageAdminCreateDTO(
        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 1, max = 2000, message = "Le message doit contenir entre 1 et 2000 caractères")
        String contenu,

        @NotNull(message = "Le type de destinataire est obligatoire")
        @Pattern(regexp = "TOUS|CIBLES", message = "Le type de destinataire doit être TOUS ou CIBLES")
        String destinataireType,

        List<Long> destinataireIds) {
}
