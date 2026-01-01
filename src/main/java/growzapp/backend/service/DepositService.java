// src/main/java/growzapp/backend/service/DepositService.java (VERSION CORRIGÉE)

package growzapp.backend.service;

import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.WalletRepository;
// Import du Record/Classe interne de PayDunyaService
import growzapp.backend.service.PayDunyaService.PayDunyaResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepositService {

    private final StripeDepositService stripeDepositService;
    private final PayDunyaService payDunyaService;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    // ... (initierDepotCarte inchangé) ...
    public String initierDepotCarte(Long userId, double montantDouble) {
        // Simplicité : appel direct à Stripe pour obtenir l'URL de paiement.
        return stripeDepositService.createCheckoutSession(userId, montantDouble);
    }

    /**
     * Crée une session PayDunya pour le dépôt par Mobile Money.
     * Enregistre la transaction EN_ATTENTE_PAIEMENT et le Token de facture
     * PayDunya.
     * Le crédit du wallet sera géré par le webhook PayDunya.
     * * @return URL de redirection PayDunya.
     */
    @Transactional
    public String initierDepotMobileMoney(Long userId, BigDecimal montant) {
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif.");
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        // 1. Appel PayDunya pour obtenir l'URL et le Token
        // Le service retourne l'objet structuré PayDunyaResponse (URL + Token)
        PayDunyaResponse payDunyaRes = payDunyaService.createDepositCheckoutSession(montant, userId);

        // 2. Enregistrement de la Transaction EN_ATTENTE_PAIEMENT (Trace)
        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.DEPOT)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Dépôt Mobile Money - Paiement PayDunya en cours.")
                .createdAt(LocalDateTime.now())
                // ** CORRECTION CLÉ : Enregistrement du Token de facture PayDunya **
                .referenceExterne(payDunyaRes.invoiceToken())
                .build();

        transactionRepository.save(tx);

        // Retourne l'URL pour la redirection du Frontend
        return payDunyaRes.redirectUrl();
    }

    // --- Méthodes Webhook de Finalisation (seraient dans un WebhookController,
    // appelant ici) ---

    /**
     * Crédite le wallet et marque la transaction comme SUCCESS (Appelé par Webhook
     * après paiement réussi par Stripe ou PayDunya)
     */
    @Transactional
    public void finaliserDepot(Long userId, BigDecimal montant, String reference, String source) {
        Wallet wallet = walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable pour la finalisation"));

        // Crédit du solde
        wallet.crediterDisponible(montant);
        walletRepository.save(wallet);

        // Mise à jour de la Transaction (ou création si la trace initiale n'a pas été
        // faite)
        // Note: Dans un vrai système, on chercherait la transaction EN_ATTENTE par
        // référence.

        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(montant)
                .type(TypeTransaction.DEPOT)
                .statut(StatutTransaction.SUCCESS)
                .description("Dépôt réussi via " + source)
                .referenceType(source)
                .referenceId(reference != null ? Long.valueOf(reference) : null) // si PayDunya/Stripe donne un ID
                .build();
        transactionRepository.save(tx);
    }
}