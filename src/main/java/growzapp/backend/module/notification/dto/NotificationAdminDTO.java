package growzapp.backend.module.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Notification envoyée à un utilisateur, avec les informations du destinataire (vue admin)")
public record NotificationAdminDTO(
        @Schema(example = "42") Long id,
        @Schema(example = "Dividende reçu") String title,
        String content,
        @Schema(example = "2025-06-15T14:30:00") LocalDateTime date,
        @Schema(example = "false") boolean isRead,
        @Schema(example = "Documents KYC insuffisants") String motif,
        @Schema(description = "Nom complet du destinataire", example = "John Doe") String destinataireNom,
        @Schema(description = "Email du destinataire", example = "john.doe@example.com") String destinataireEmail) {
}
