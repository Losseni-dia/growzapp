package growzapp.backend.module.fournisseur.dto;

import growzapp.backend.module.fournisseur.enums.StatutJuridiqueFournisseur;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Brouillon de fiche fournisseur — aucun champ n'est obligatoire, sauvegardable à tout moment avant soumission finale")
public record FournisseurBrouillonDTO(
        StatutJuridiqueFournisseur statutJuridique,
        String raisonSociale,
        String secteurNom,
        String ville,
        String pays,
        String telephone,
        String email,
        String description) {
}
