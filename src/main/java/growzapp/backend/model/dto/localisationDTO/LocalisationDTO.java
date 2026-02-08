// src/main/java/growzapp/backend/model/dto/localisationDTO/LocalisationDTO.java
package growzapp.backend.model.dto.localisationDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LocalisationDTO(
                Long id,
                String nom,
                String adresse,
                String contact,
                String responsable,
                LocalDateTime createdAt,
                // --- PARTIE GÉO ---
                BigDecimal latitude,
                BigDecimal longitude,
                String what3words,
                String googleMapsUrl,
                // --- RELATIONS ---
                String localiteNom,
                Long localiteId,
                String paysNom,
                List<String> projets) {
}