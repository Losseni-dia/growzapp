package growzapp.backend.module.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Représentation d'un document pour la vue admin globale, incluant le projet concerné")
public record DocumentAdminDTO(
        Long id,
        String nom,
        String url,
        String type,
        String description,
        String statut,
        LocalDateTime uploadedAt,
        Long projetId,
        String projetLibelle
) {
}