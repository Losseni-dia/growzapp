package growzapp.backend.module.dividende.dto;

import growzapp.backend.module.dividende.enums.StatutDividende;
import growzapp.backend.module.facture.dto.FactureDTO;
import growzapp.backend.module.paiement.enums.MoyenPaiement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Représentation d'un dividende lié à un investissement")
public record DividendeDTO(
        @Schema(description = "Identifiant unique du dividende", example = "33")
        Long id,

        @Schema(description = "Montant du dividende par part détenue", type = "number", format = "double", example = "75.00")
        BigDecimal montantParPart,

        @Schema(description = "Statut du dividende",
                example = "PLANIFIE",
                allowableValues = {"PLANIFIE", "PAYE", "ANNULE"})
        StatutDividende statutDividende,

        @Schema(description = "Moyen de paiement utilisé pour ce dividende",
                example = "WALLET",
                allowableValues = {"VIREMENT", "CARTE", "MOBILE_MONEY", "ORANGE_MONEY", "WAVE", "CRYPTO", "WALLET"})
        MoyenPaiement moyenPaiement,

        @Schema(description = "Date de paiement effective ou planifiée", example = "2025-12-31")
        LocalDate datePaiement,

        @Schema(description = "Identifiant de l'investissement auquel ce dividende est rattaché", example = "15")
        Long investissementId,

        @Schema(description = "Informations textuelles sur l'investissement (projet + investisseur)", example = "Ferme solaire — John Doe")
        String investissementInfo,

        @Schema(description = "Montant total du dividende pour toutes les parts de cet investissement", type = "number", format = "double", example = "375.00")
        BigDecimal montantTotal,

        @Schema(description = "Nom du fichier PDF de la facture (usage interne)", example = "facture-FACT-2025-00033.pdf")
        String fileName,

        @Schema(description = "URL directe du PDF de la facture téléchargeable", example = "/uploads/factures/facture-FACT-2025-00033.pdf")
        String factureUrl,

        @Schema(description = "Détail complet de la facture associée à ce dividende")
        FactureDTO facture,

        @Schema(description = "Motif ou description du versement de ce dividende", example = "Dividendes T4 2025 — Ferme solaire Bobo-Dioulasso")
        String motif
) {
}
