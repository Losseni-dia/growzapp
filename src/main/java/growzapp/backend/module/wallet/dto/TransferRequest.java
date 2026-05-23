package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps de la requête de transfert de fonds entre deux utilisateurs")
public record TransferRequest(
        @Schema(description = "Identifiant de l'utilisateur destinataire", example = "55")
        Long destinataireUserId,

        @Schema(description = "Montant à transférer", type = "number", format = "double", example = "100.00")
        double montant,

        @Schema(description = "Source du solde à utiliser pour le transfert", example = "DISPONIBLE",
                allowableValues = {"DISPONIBLE", "RETIRABLE"})
        String source
) {
    public TransferRequest {
        if (destinataireUserId == null || destinataireUserId <= 0)
            throw new IllegalArgumentException("Destinataire invalide");
        if (montant <= 0)
            throw new IllegalArgumentException("Montant doit être positif");
        if (source == null || (!source.equals("DISPONIBLE") && !source.equals("RETIRABLE")))
            throw new IllegalArgumentException("Source doit être 'DISPONIBLE' ou 'RETIRABLE'");
    }
}
