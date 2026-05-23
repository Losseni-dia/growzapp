package growzapp.backend.module.wallet.dto;

import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Représentation complète d'une transaction financière sur la plateforme")
public record TransactionDTO(
        @Schema(description = "Identifiant unique de la transaction", example = "101")
        Long id,

        @Schema(description = "Montant de la transaction", type = "number", format = "double", example = "250.00")
        BigDecimal montant,

        @Schema(description = "Type de la transaction",
                example = "DEPOT",
                allowableValues = {"DEPOT", "RETRAIT", "TRANSFER_OUT", "TRANSFER_IN", "INVESTISSEMENT",
                        "PAIEMENT_STRIPE", "PAIEMENT_OM", "PAIEMENT_MTN", "PAIEMENT_WAVE",
                        "REMBOURSEMENT", "PAYOUT_STRIPE", "PAYOUT_OM", "PAYOUT_WAVE",
                        "CREDIT_PROJET", "VERSEMENT_DIVIDENDE", "DIVIDENDE_ENTRANT", "DIVIDENDE_SORTANT"})
        TypeTransaction type,

        @Schema(description = "Statut courant de la transaction",
                example = "SUCCESS",
                allowableValues = {"EN_COURS", "SUCCESS", "FAILED", "EN_ATTENTE_VALIDATION",
                        "REJETEE", "EN_ATTENTE_PAIEMENT", "PAYE", "ECHEC_PAIEMENT"})
        StatutTransaction statut,

        @Schema(description = "Date et heure de création de la transaction", example = "2025-11-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Date et heure de finalisation de la transaction (null si en cours)", example = "2025-11-15T10:35:00")
        LocalDateTime completedAt,

        @Schema(description = "Description ou motif de la transaction", example = "Dépôt via Stripe — session cs_test_abc123")
        String description,

        @Schema(description = "Identifiant de l'utilisateur propriétaire du wallet", example = "42")
        Long userId,

        @Schema(description = "Prénom de l'utilisateur propriétaire", example = "John")
        String userPrenom,

        @Schema(description = "Nom de l'utilisateur propriétaire", example = "Doe")
        String userNom,

        @Schema(description = "Login de l'utilisateur propriétaire", example = "john.doe")
        String userLogin,

        @Schema(description = "Identifiant du destinataire (pour les transferts)", example = "55")
        Long destinataireUserId,

        @Schema(description = "Nom complet du destinataire", example = "Marie Dupont")
        String destinataireNomComplet,

        @Schema(description = "Login du destinataire", example = "marie.dupont")
        String destinataireLogin,

        @Schema(description = "Identifiant de l'expéditeur (pour les transferts reçus)", example = "42")
        Long expediteurUserId,

        @Schema(description = "Nom complet de l'expéditeur", example = "John Doe")
        String expediteurNomComplet,

        @Schema(description = "Login de l'expéditeur", example = "john.doe")
        String expediteurLogin
) {
}
