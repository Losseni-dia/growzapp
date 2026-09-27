package growzapp.backend.module.growzmarket.dto;

import java.time.LocalDateTime;

public record LitigeMessageDTO(
        Long id,
        Long auteurId,
        String auteurNom,
        String role,
        String contenu,
        String destinataire,
        LocalDateTime dateEnvoi) {
}
