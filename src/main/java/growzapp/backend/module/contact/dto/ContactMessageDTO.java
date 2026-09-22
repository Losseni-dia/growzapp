package growzapp.backend.module.contact.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ContactMessageDTO(
        Long id,
        Long userId,
        String userNom,
        String userEmail,
        String sujet,
        String message,
        String statut,
        LocalDateTime dateEnvoi,
        List<ContactReplyDTO> reponses) {
}
