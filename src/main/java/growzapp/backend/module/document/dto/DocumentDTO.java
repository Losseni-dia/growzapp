package growzapp.backend.module.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Représentation d'un document attaché à un projet (PDF, Excel, CSV)")
public record DocumentDTO(
        @Schema(description = "Identifiant unique du document", example = "9")
        Long id,

        @Schema(description = "Nom affiché du document", example = "Budget prévisionnel 2025")
        String nom,

        @Schema(description = "URL de téléchargement du fichier", example = "/files/documents/uuid_budget.xlsx")
        String url,

        @Schema(description = "Type du document", example = "EXCEL", allowableValues = {"PDF", "EXCEL", "CSV"})
        String type,

        @Schema(description = "Date et heure d'upload du document", example = "2025-09-10T14:30:00")
        LocalDateTime uploadedAt
) {
}
