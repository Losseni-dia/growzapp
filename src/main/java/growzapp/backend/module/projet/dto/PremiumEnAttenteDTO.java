package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Achat Premium bloqué en attente de paiement (webhook jamais reçu)")
public record PremiumEnAttenteDTO(
        @Schema(description = "Identifiant de la transaction d'initiation") Long transactionId,
        @Schema(description = "Identifiant du projet concerné") Long projetId,
        @Schema(description = "Libellé du projet") String projetLibelle,
        @Schema(description = "Montant de l'achat Premium") BigDecimal montant,
        @Schema(description = "Date d'initiation du paiement") LocalDateTime createdAt,
        @Schema(description = "Origine du paiement (MOBILE_MONEY, CARTE_BANCAIRE)") String sourcePaiement) {
}
