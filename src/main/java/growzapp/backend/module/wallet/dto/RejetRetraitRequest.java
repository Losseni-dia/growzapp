package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps de la requête de rejet d'une demande de retrait (Admin)")
public record RejetRetraitRequest(
        @Schema(description = "Motif du rejet communiqué à l'utilisateur", example = "Informations bancaires incomplètes ou incorrectes.")
        String motif
) {
    public RejetRetraitRequest {
        if (motif == null || motif.trim().isBlank())
            throw new IllegalArgumentException("Le motif de rejet est obligatoire");
    }
}
