// src/main/java/growzapp/backend/model/dto/localisationDTO/LocalisationDTO.java
package growzapp.backend.model.dto.localisationDTO;

import java.time.LocalDateTime;
import java.util.List;

public record LocalisationDTO(
        Long id,
        String nom,
        String adresse,
        String contact,
        String responsable,
        LocalDateTime createdAt,
        String localiteNom,
        Long localiteId,
        String paysNom, // AJOUTÉ
        List<String> projets) {
}