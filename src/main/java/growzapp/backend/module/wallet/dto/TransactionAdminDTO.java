package growzapp.backend.module.wallet.dto;

import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Ligne de l'historique global des transactions (dépôts, retraits, investissements, versements porteur, dividendes...), vue admin")
public record TransactionAdminDTO(
        @Schema(example = "1024") Long id,
        @Schema(description = "Type de transaction", example = "INVESTISSEMENT") TypeTransaction type,
        @Schema(description = "Type de wallet source (USER, PROJET, DIVIDENDE)", example = "USER") WalletType walletType,
        @Schema(example = "50000.00") BigDecimal montant,
        StatutTransaction statut,
        @Schema(description = "Date de création de la transaction") LocalDateTime createdAt,
        @Schema(description = "Date de complétion, si terminée") LocalDateTime completedAt,
        @Schema(description = "Description libre de la transaction") String description,
        @Schema(description = "Type de l'entité source liée (ex: INVESTISSEMENT, PROJET)") String referenceType,
        @Schema(description = "Identifiant de l'entité source liée") Long referenceId,
        @Schema(description = "Référence externe (fournisseur de paiement)") String referenceExterne,
        @Schema(description = "Nom complet du titulaire du wallet source, si wallet utilisateur") String utilisateurNom,
        @Schema(description = "Email du titulaire du wallet source, si wallet utilisateur") String utilisateurEmail,
        @Schema(description = "Libellé du projet lié, si wallet projet") String projetLibelle,
        @Schema(description = "Nom complet du destinataire, pour les virements internes") String destinataireNom
) {
}
