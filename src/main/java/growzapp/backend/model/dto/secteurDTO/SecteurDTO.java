package growzapp.backend.model.dto.secteurDTO;

import java.util.List;

public record SecteurDTO(
        Long id,
        String nom,
        List<String> projets // noms des projets (ou ProjetDTO si tu veux plus tard)
) {
}