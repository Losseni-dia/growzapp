package growzapp.backend.module.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Notification envoyée à un utilisateur de la plateforme")
public record NotificationDTO(
        @Schema(example = "42") Long id,
        @Schema(example = "Dividende reçu") String title,
        String content,
        @Schema(example = "2025-06-15T14:30:00") LocalDateTime date,
        @Schema(example = "false") boolean isRead,
        @Schema(example = "7") Long projetId,
        @Schema(example = "ferme-solaire-nord") String projetSlug,
        @Schema(example = "22") Long factureId,
        @Schema(example = "Documents KYC insuffisants") String motif) {
}