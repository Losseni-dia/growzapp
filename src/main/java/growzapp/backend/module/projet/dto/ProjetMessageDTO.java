package growzapp.backend.module.projet.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record ProjetMessageDTO(
        Long id,
        Long auteurId,
        String auteurNom,
        String role,
        String contenu,
        Set<Long> destinataireIds,
        LocalDateTime dateEnvoi) {
}
