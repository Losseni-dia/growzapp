// src/main/java/growzapp/backend/service/WithdrawalService.java

package growzapp.backend.service;

import growzapp.backend.model.entite.PayoutModel;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final StripePayoutService stripePayoutService; // Payout vers compte bancaire
    private final PayDunyaService payDunyaService; // Payout vers Mobile Money
    private final WalletRepository walletRepository;
    private final PayoutModelRepository payoutModelRepository;

    /**
     * Outil interne pour récupérer le wallet avec verrou pessimiste.
     */
    private Wallet getWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé (user ID: " + userId + ")"));
    }

    // ====================================================================
    // 1. RETRAIT BANCAIRE (via Stripe Payout)
    // ====================================================================
    /**
     * Exécute un retrait direct vers un compte bancaire (via Stripe Payout).
     * Les fonds sont DÉBITÉS du solde retirable immédiatement.
     * * @param userId ID de l'utilisateur.
     * 
     * @param montant Montant à retirer.
     * @param phone   Numéro de téléphone de l'utilisateur (utilisé pour la
     *                trace/métadonnées).
     * @return ID de référence du Payout Stripe.
     */
    @Transactional
    public String executerRetraitBancaire(Long userId, BigDecimal montant, String phone) {
        Wallet wallet = getWalletWithLock(userId);

        if (wallet.getSoldeRetirable().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde retirable insuffisant.");
        }

        // 1. DÉBIT IMMÉDIAT du solde retirable (avant l'appel API)
        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montant));
        walletRepository.save(wallet);

        // 2. Créer l'objet PayoutModel (trace)
        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .montant(montant)
                .userPhone(phone)
                .type(TypeTransaction.PAYOUT_STRIPE)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .build();
        payout = payoutModelRepository.save(payout);

        // 3. Appel à l'API Stripe Payout
        try {
            // CORRECTION: Utilisation du nom de méthode correct avec les bons paramètres.
            String stripePayoutId = stripePayoutService.createBankPayoutWithNewTransaction(userId, montant, phone);

            // Mise à jour de la trace après succès de l'appel Stripe
            payout.setExternalPayoutId(stripePayoutId);
            // Le statut final sera mis à jour par le Webhook Stripe (SUCCESS ou FAILED).
            // Pour l'instant, nous le laissons à EN_ATTENTE_PAIEMENT jusqu'au webhook.
            payoutModelRepository.save(payout);

            return stripePayoutId;
        } catch (Exception e) {
            // Si l'appel API Stripe échoue, la transaction DB précédente (débit) est
            // annulée
            // grâce à @Transactional.
            log.error("Échec du Payout Stripe, rollback en cours.", e);
            throw new RuntimeException("Échec de la transaction de retrait bancaire: " + e.getMessage());
        }
    }

    // ====================================================================
    // 2. RETRAIT MOBILE MONEY (via PayDunya)
    // ====================================================================
    /**
     * Exécute un retrait direct vers Mobile Money (via PayDunya Disburse/Payout).
     * * @param userId ID de l'utilisateur.
     * 
     * @param montant Montant à retirer.
     * @param mmType  Type de transaction Mobile Money (OM, MTN, Wave).
     * @param phone   Numéro de téléphone destinataire.
     * @return ID de référence de la transaction PayDunya.
     */
    @Transactional
    public String executerRetraitMobileMoney(Long userId, BigDecimal montant, TypeTransaction mmType, String phone) {
        Wallet wallet = getWalletWithLock(userId);

        if (wallet.getSoldeRetirable().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde retirable insuffisant.");
        }

        // 1. DÉBIT IMMÉDIAT du solde retirable
        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montant));
        walletRepository.save(wallet);

        // 2. Créer l'objet PayoutModel (trace)
        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .montant(montant)
                .userPhone(phone)
                .type(mmType) // PAYOUT_OM, PAYOUT_MTN, etc.
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .build();
        payout = payoutModelRepository.save(payout);

        // 3. Appel à l'API PayDunya (Disbursement / Payout)
        try {
            // L'ID du PayoutModel est passé pour être utilisé comme référence externe dans
            // PayDunya.
            String txId = payDunyaService.initiatePayout(montant, phone, mmType, payout.getId());

            // Met à jour la référence externe PayDunya
            payout.setExternalPayoutId(txId);
            payoutModelRepository.save(payout);

            // Le statut final sera mis à jour par le Webhook PayDunya.
            return txId;
        } catch (Exception e) {
            // Si l'appel API PayDunya échoue, rollback de la transaction DB (débit du
            // solde)
            log.error("Échec du Payout Mobile Money, rollback en cours.", e);
            throw new RuntimeException("Échec de la transaction de retrait Mobile Money: " + e.getMessage());
        }
    }
}