package growzapp.backend.module.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Envoi d'un message de contact/support par un visiteur non connecté")
public record ContactMessagePublicCreateDTO(

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        @Schema(example = "visiteur@example.com") String email,

        @NotBlank(message = "Le sujet est obligatoire")
        @Size(min = 3, max = 150, message = "Le sujet doit contenir entre 3 et 150 caractères")
        @Schema(example = "Question sur mon compte") String sujet,

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 10, max = 3000, message = "Le message doit contenir entre 10 et 3000 caractères")
        @Schema(example = "Bonjour, mon compte a été supprimé et...") String message) {
}
