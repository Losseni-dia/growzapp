package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps de la requête de clôture en échec d'un projet (remboursement intégral des investisseurs)")
public record CloturerEchecRequest(
        @Schema(description = "Motif de la clôture (optionnel)", example = "Objectif non atteint à la date limite")
        String motif
) {
}
