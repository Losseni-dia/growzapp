package growzapp.backend.model.dto.paysDTO;

import java.util.List;

public record PaysDTO(
        Long id,
        String nom,
        List<String> localites // noms des localités
) {
}
