package growzapp.backend.module.contact.dto;

import java.time.LocalDateTime;

public record ContactReplyDTO(
        Long id,
        Long auteurId,
        String auteurNom,
        boolean isAdmin,
        String contenu,
        LocalDateTime dateEnvoi) {
}
