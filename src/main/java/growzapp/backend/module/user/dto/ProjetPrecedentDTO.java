package growzapp.backend.module.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Projet réel du porteur mis en avant sur sa fiche de présentation — libellé déjà traduit selon la langue demandée, statut à traduire côté client via i18n")
public record ProjetPrecedentDTO(
        Long id,
        String libelle,
        String statutProjet,
        Integer pourcentageFinance
) {
}
