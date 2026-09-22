package growzapp.backend.module.fournisseur.dto;

import java.time.LocalDateTime;

public record FournisseurDTO(
        Long id,
        Long userId,
        String nomContact,
        String statutJuridique,
        String raisonSociale,
        Long secteurId,
        String secteurNom,
        String ville,
        String pays,
        String telephone,
        String email,
        String description,
        String statut,
        LocalDateTime dateSoumission,
        LocalDateTime dateValidation,
        String motifRejet,
        String logoUrl) {
}
