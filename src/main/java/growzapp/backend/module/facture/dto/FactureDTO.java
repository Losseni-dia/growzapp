package growzapp.backend.module.facture.dto;

import growzapp.backend.module.facture.enums.StatutFacture;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Représentation d'une facture de dividende")
public record FactureDTO(
        @Schema(description = "Identifiant unique de la facture", example = "22")
        Long id,

        @Schema(description = "Numéro officiel de la facture", example = "FAC-2025-000022")
        String numeroFacture,

        @Schema(description = "Montant hors taxes", type = "number", format = "double", example = "1500.00")
        Double montantHT,

        @Schema(description = "Taux ou montant TVA", type = "number", format = "double", example = "0.0")
        Double tva,

        @Schema(description = "Montant total toutes taxes comprises", type = "number", format = "double", example = "1500.00")
        Double montantTTC,

        @Schema(description = "Date d'émission de la facture", example = "2025-09-10T08:00:00")
        LocalDateTime dateEmission,

        @Schema(description = "Date de paiement effective", example = "2025-09-15T00:00:00")
        LocalDateTime datePaiement,

        @Schema(description = "Statut de la facture", example = "PAYEE")
        StatutFacture statut,

        @Schema(description = "Identifiant du dividende lié", example = "8")
        Long dividendeId,

        @Schema(description = "Identifiant de l'investisseur", example = "42")
        Long investisseurId,

        @Schema(description = "Nom complet de l'investisseur", example = "John Doe")
        String investisseurNom,

        @Schema(description = "URL du fichier PDF stocké", example = "/uploads/factures/facture-dividende-8.pdf")
        String fichierUrl
) {
}
