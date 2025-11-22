package growzapp.backend.model.dto.documentDTO;


import java.time.LocalDateTime;

public record DocumentDTO(
        Long id,
        String nom,
        String url,
        String type,
        LocalDateTime uploadedAt) {
}
