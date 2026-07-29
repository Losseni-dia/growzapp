package growzapp.backend.module.paiement.common;

import java.math.BigDecimal;

/**
 * Interface commune à tous les fournisseurs de paiement (FedaPay, PayDunya,
 * Stripe...). Le code métier ne dépend que de cette interface, jamais
 * directement d'un fournisseur précis — permet le fallback automatique.
 */
public interface PaymentProviderService {

    /** Nom du fournisseur, pour les logs et le choix du routeur. */
    String getProviderName();

    /** Dépôt simple sur le wallet de l'utilisateur. */
    PaymentSessionResponse creerSessionDepot(BigDecimal montant, Long userId);

    /** Investissement direct dans un projet (montant bloqué). */
    PaymentSessionResponse creerSessionInvestissement(
            BigDecimal montant,
            Long userId,
            Long projetId,
            int nombreParts,
            String projetLibelle,
            String projetSlug);

    /** Décaissement (retrait) vers un numéro Mobile Money. */
    PayoutResponse initierRetrait(
            BigDecimal montant,
            String phone,
            String moyenPaiement,
            Long referenceId);

    record PaymentSessionResponse(String redirectUrl, String sessionToken) {
    }

    record PayoutResponse(String payoutToken, String externalTxId, String status) {
    }
}