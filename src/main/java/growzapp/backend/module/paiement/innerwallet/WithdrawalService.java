package growzapp.backend.module.paiement.innerwallet;

import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.paiement.model.PayoutModel;
import growzapp.backend.module.paiement.paydunya.PayDunyaService;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
import growzapp.backend.module.paiement.stripe.StripePayoutService;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
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

    private final StripePayoutService stripePayoutService;
    private final PayDunyaService payDunyaService;
    private final WalletRepository walletRepository;
    private final PayoutModelRepository payoutModelRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private Wallet getWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé (user ID: " + userId + ")"));
    }

    // ════════════════════════════════════════════════════════════════════
    // 1. RETRAIT BANCAIRE (Stripe Payout) — automatique, sans validation admin
    // ════════════════════════════════════════════════════════════════════
    @Transactional
    public String executerRetraitBancaire(Long userId, BigDecimal montant, String phone, String idempotencyKey) {
        // Rejette immédiatement si cette clé a déjà été utilisée — protège
        // contre le double-clic ou une requête réseau relancée (HIGH-06)
        if (idempotencyKey != null && payoutModelRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        Wallet wallet = getWalletWithLock(userId);
        if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant.");
        }

        // 1. Débit immédiat du solde disponible
        wallet.setSoldeDisponible(wallet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(wallet);

        // 2. Trace PayoutModel
        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .montant(montant)
                .userPhone(phone)
                .type(TypeTransaction.PAYOUT_STRIPE)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();
        payout = payoutModelRepository.save(payout);

        // 3. Appel réel Stripe Payout
        try {
            String stripePayoutId = stripePayoutService.createBankPayoutDirect(userId, montant, payout.getId());
            payout.setExternalPayoutId(stripePayoutId);
            payout.setStatut(StatutTransaction.SUCCESS);
            payout.setCompletedAt(LocalDateTime.now());
            payoutModelRepository.save(payout);
            log.info("Retrait bancaire réussi : user={} montant={} payoutId={}", userId, montant, stripePayoutId);
            return stripePayoutId;
        } catch (Exception e) {
            // ── REMBOURSEMENT AUTOMATIQUE sur soldeDisponible ──────────────
            rembourserEtNotifier(userId, montant, payout, e);
            throw new RuntimeException("Échec de la transaction de retrait bancaire : " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. RETRAIT MOBILE MONEY (PayDunya Disburse) — automatique
    // ════════════════════════════════════════════════════════════════════
    @Transactional
    public String executerRetraitMobileMoney(Long userId, BigDecimal montant, TypeTransaction mmType, String phone,
            String idempotencyKey) {
        // Rejette immédiatement si cette clé a déjà été utilisée — protège
        // contre le double-clic ou une requête réseau relancée (HIGH-06)
        if (idempotencyKey != null && payoutModelRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        Wallet wallet = getWalletWithLock(userId);
        if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant.");
        }

        // 1. Débit immédiat du solde disponible
        wallet.setSoldeDisponible(wallet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(wallet);

        // 2. Trace PayoutModel
        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .montant(montant)
                .userPhone(phone)
                .type(mmType)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();
        payout = payoutModelRepository.save(payout);

        // 3. Appel réel PayDunya Disburse
        try {
            String txId = payDunyaService.initiatePayout(montant, phone, mmType, payout.getId());
            payout.setExternalPayoutId(txId);
            payout.setStatut(StatutTransaction.SUCCESS);
            payout.setCompletedAt(LocalDateTime.now());
            payoutModelRepository.save(payout);
            log.info("Retrait Mobile Money réussi : user={} montant={} txId={}", userId, montant, txId);
            return txId;
        } catch (Exception e) {
            // ── REMBOURSEMENT AUTOMATIQUE sur soldeDisponible ──────────────
            rembourserEtNotifier(userId, montant, payout, e);
            throw new RuntimeException("Échec de la transaction de retrait Mobile Money : " + e.getMessage(), e);
        }
    }

    // ── Remboursement automatique + notification en cas d'échec ─────────────
    // ── Remboursement automatique + notification en cas d'échec ─────────────
    // REQUIRES_NEW : doit survivre même si la transaction appelante est
    // annulée par l'exception relancée juste après (bug trouvé lors du test
    // d'idempotence — sans ça, toute la transaction (dont ce remboursement
    // et la clé d'idempotence elle-même) était silencieusement annulée).
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void rembourserEtNotifier(Long userId, BigDecimal montant, PayoutModel payout, Exception e) {
        log.error("Échec payout user={} montant={} — remboursement automatique", userId, montant, e);
        Wallet walletRefund = getWalletWithLock(userId);
        walletRefund.setSoldeDisponible(walletRefund.getSoldeDisponible().add(montant));
        walletRepository.saveAndFlush(walletRefund);
        payout.setStatut(StatutTransaction.ECHEC_PAIEMENT);
        payoutModelRepository.save(payout);
        userRepository.findById(userId).ifPresent(user -> notificationService.notifyUser(
                user,
                "❌ Échec du retrait",
                "Votre retrait de " + montant.toPlainString()
                        + " FCFA a échoué. Les fonds ont été remboursés dans votre portefeuille GrowzApp.",
                null, null,
                e.getMessage()));
    }
}