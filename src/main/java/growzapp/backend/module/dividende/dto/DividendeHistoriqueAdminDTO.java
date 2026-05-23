package growzapp.backend.module.dividende.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Résumé historique d'un dividende pour la vue administrateur")
public record DividendeHistoriqueAdminDTO(
        @Schema(description = "Identifiant unique du dividende", example = "33")
        Long id,

        @Schema(description = "Montant total versé pour ce dividende", type = "number", format = "double", example = "375.00")
        Double montantTotal,

        @Schema(description = "Date de paiement au format ISO ou formatée", example = "2025-12-31")
        String datePaiement,

        @Schema(description = "Motif ou description du versement", example = "Dividendes T4 2025 — Ferme solaire Bobo-Dioulasso")
        String motif,

        @Schema(description = "Nom complet de l'investisseur bénéficiaire", example = "John Doe")
        String investisseurNom,

        @Schema(description = "URL du PDF de la facture (null si non générée)", example = "/uploads/factures/facture-FACT-2025-00033.pdf")
        String factureUrl
) {
}
