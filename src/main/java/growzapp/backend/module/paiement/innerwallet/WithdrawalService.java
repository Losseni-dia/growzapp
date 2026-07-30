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
import org.springframework.transaction.annotation.Propagation;
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
    public String executerRetraitBancaire(Long userId, BigDecimal montant, String phone, String idempotencyKey) {
        if (idempotencyKey != null && payoutModelRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        // Débit + création du payout validés IMMÉDIATEMENT, dans leur propre
        // transaction — indépendants de ce qui se passe avec Stripe ensuite
        // (bug trouvé lors du test HIGH-06 : réutiliser un objet créé dans
        // une transaction annulée dans une transaction séparée ne fonctionne
        // pas — Hibernate ne voit pas la ligne).
        PayoutModel payout = debiterEtCreerPayout(userId, montant, phone, TypeTransaction.PAYOUT_STRIPE,
                idempotencyKey);

        try {
            String stripePayoutId = stripePayoutService.createBankPayoutDirect(userId, montant, payout.getId());
            marquerSucces(payout.getId(), stripePayoutId);
            log.info("Retrait bancaire réussi : user={} montant={} payoutId={}", userId, montant, stripePayoutId);
            return stripePayoutId;
        } catch (Exception e) {
            rembourserEtNotifier(userId, montant, payout.getId(), e);
            throw new RuntimeException("Échec de la transaction de retrait bancaire : " + e.getMessage(), e);
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. RETRAIT MOBILE MONEY (PayDunya / FedaPay Disburse) — automatique
    // ════════════════════════════════════════════════════════════════════
    public String executerRetraitMobileMoney(Long userId, BigDecimal montant, TypeTransaction mmType, String phone,
            String idempotencyKey) {
        if (idempotencyKey != null && payoutModelRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new IllegalStateException("Cette demande de retrait a déjà été traitée.");
        }

        PayoutModel payout = debiterEtCreerPayout(userId, montant, phone, mmType, idempotencyKey);

        try {
            String txId = payDunyaService.initiatePayout(montant, phone, mmType, payout.getId());
            marquerSucces(payout.getId(), txId);
            log.info("Retrait Mobile Money réussi : user={} montant={} txId={}", userId, montant, txId);
            return txId;
        } catch (Exception e) {
            rembourserEtNotifier(userId, montant, payout.getId(), e);
            throw new RuntimeException("Échec de la transaction de retrait Mobile Money : " + e.getMessage(), e);
        }
    }

    // ── Débit + création du PayoutModel — transaction courte, validée avant
    // tout appel au fournisseur externe ─────────────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PayoutModel debiterEtCreerPayout(Long userId, BigDecimal montant, String phone,
            TypeTransaction type, String idempotencyKey) {
        Wallet wallet = getWalletWithLock(userId);
        if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
            throw new IllegalArgumentException("Solde disponible insuffisant.");
        }

        wallet.setSoldeDisponible(wallet.getSoldeDisponible().subtract(montant));
        walletRepository.saveAndFlush(wallet);

        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .montant(montant)
                .userPhone(phone)
                .type(type)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .idempotencyKey(idempotencyKey)
                .build();
        return payoutModelRepository.saveAndFlush(payout);
    }

    // ── Marque le payout comme réussi — transaction séparée ─────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marquerSucces(Long payoutId, String externalId) {
        payoutModelRepository.findById(payoutId).ifPresent(p -> {
            p.setExternalPayoutId(externalId);
            p.setStatut(StatutTransaction.SUCCESS);
            p.setCompletedAt(LocalDateTime.now());
            payoutModelRepository.save(p);
        });
    }

    // ── Remboursement automatique + notification en cas d'échec — recharge
    // le payout par ID depuis la base plutôt que de réutiliser un objet
    // détaché d'une autre transaction ────────────────────────────────────────
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rembourserEtNotifier(Long userId, BigDecimal montant, Long payoutId, Exception e) {
        log.error("Échec payout user={} montant={} — remboursement automatique", userId, montant, e);
        Wallet walletRefund = getWalletWithLock(userId);
        walletRefund.setSoldeDisponible(walletRefund.getSoldeDisponible().add(montant));
        walletRepository.saveAndFlush(walletRefund);

        payoutModelRepository.findById(payoutId).ifPresent(p -> {
            p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
            payoutModelRepository.save(p);
        });

        userRepository.findById(userId).ifPresent(user -> notificationService.notifyUser(
                user,
                "❌ Échec du retrait",
                "Votre retrait de " + montant.toPlainString()
                        + " FCFA a échoué. Les fonds ont été remboursés dans votre portefeuille GrowzApp.",
                null, null,
                e.getMessage()));
    }
}