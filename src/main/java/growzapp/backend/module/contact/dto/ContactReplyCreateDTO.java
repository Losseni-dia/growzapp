package growzapp.backend.module.contact.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nouveau message dans un fil de contact/support déjà existant")
public record ContactReplyCreateDTO(

        @NotBlank(message = "Le message est obligatoire")
        @Size(min = 3, max = 3000, message = "Le message doit contenir entre 3 et 3000 caractères")
        @Schema(example = "Merci, mais j'ai une autre question concernant...") String message) {
}
