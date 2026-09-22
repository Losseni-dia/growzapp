package growzapp.backend.module.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Envoi d'un message de contact/support")
public record ContactMessageCreateDTO(

        @NotBlank(message = "Le sujet est obligatoire")
        @Size(min = 3, max = 150, message = "Le sujet doit contenir entre 3 et 150 caractères")
        @Schema(example = "Problème lors d'un dépôt") String sujet,

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 10, max = 3000, message = "Le message doit contenir entre 10 et 3000 caractères")
        @Schema(example = "Bonjour, mon dépôt de 10 000 FCFA reste en attente depuis hier...") String message) {
}
