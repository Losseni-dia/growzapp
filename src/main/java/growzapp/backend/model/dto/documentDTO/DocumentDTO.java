// src/main/java/growzapp/backend/model/dto/documentDTO/DocumentDTO.java
package growzapp.backend.model.dto.documentDTO;

import java.time.LocalDateTime;

public record DocumentDTO(
                Long id,
                String nom,
                String url, // ← on garde "url" pour le frontend (c’est /files/documents/...)
                String type,
                LocalDateTime uploadedAt) {
}