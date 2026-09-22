package growzapp.backend.module.contact.dto;

import java.time.LocalDateTime;

public record ContactMessageDTO(
        Long id,
        Long userId,
        String userNom,
        String userEmail,
        String sujet,
        String message,
        String statut,
        String reponse,
        String responduPar,
        LocalDateTime dateEnvoi,
        LocalDateTime dateReponse) {
}
