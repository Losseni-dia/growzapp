package growzapp.backend.module.contrat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résultat de la vérification publique d'un contrat d'investissement")
public record ContratPublicDTO(
        @Schema(description = "Indique si le contrat est valide et les identifiants corrects", example = "true")
        boolean valide,

        @Schema(description = "Numéro officiel du contrat", example = "CONTRAT-2025-00015")
        String numeroContrat,

        @Schema(description = "Libellé du projet lié au contrat", example = "Ferme solaire Bobo-Dioulasso")
        String projet,

        @Schema(description = "Nom complet de l'investisseur", example = "John Doe")
        String investisseur,

        @Schema(description = "Montant investi indiqué sur le contrat", type = "number", format = "double", example = "2500.00")
        double montant,

        @Schema(description = "Date de génération du contrat", example = "2025-09-10")
        String date
) {
}
