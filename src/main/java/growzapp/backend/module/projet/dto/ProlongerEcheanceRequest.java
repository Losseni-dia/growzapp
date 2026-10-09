package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Corps de la requête de prolongation de la date limite de financement d'un projet")
public record ProlongerEcheanceRequest(
        @NotNull
        @Schema(description = "Nouvelle date limite de financement, doit être postérieure à l'actuelle", example = "2026-12-31")
        LocalDate nouvelleDateFin
) {
}
