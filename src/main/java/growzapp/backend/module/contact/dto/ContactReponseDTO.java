package growzapp.backend.module.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Réponse d'un administrateur à un message de contact")
public record ContactReponseDTO(

        @NotBlank(message = "La réponse est obligatoire")
        @Size(min = 3, max = 3000, message = "La réponse doit contenir entre 3 et 3000 caractères")
        @Schema(example = "Bonjour, votre dépôt a bien été crédité, merci de vérifier votre solde.") String reponse) {
}
