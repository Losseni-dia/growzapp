package growzapp.backend.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Fiche publique — volontairement dépourvue de tout moyen de contact direct
 * (téléphone, email, site web, LinkedIn, réseaux). GrowzApp reste le seul
 * intermédiaire entre investisseurs et porteurs ; ces coordonnées sont
 * collectées à la soumission (cf FichePorteurSubmitDTO) uniquement pour la
 * vérification interne par l'admin, jamais affichées aux investisseurs.
 */
@Schema(description = "Fiche de présentation publique d'un porteur de projet, visible des investisseurs connectés — aucune coordonnée de contact direct n'est exposée")
public record FichePorteurPublicDTO(
        Long porteurId,
        String nomComplet,
        String photoUrl,
        String bio,
        String statutJuridique,
        String raisonSociale,
        Integer anneesExperience,
        String projetsPrecedents,
        boolean verifie
) {
}
