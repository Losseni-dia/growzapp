package growzapp.backend.module.paiement.innerwallet;

import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.paiement.model.PayoutModel;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
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

/**
 * Extrait dans une classe séparée pour que @Transactional(REQUIRES_NEW)
 * fonctionne réellement — un appel interne (this.xxx()) depuis
 * WithdrawalService contournerait le proxy Spring et l'annotation serait
 * silencieusement ignorée (piège classique de self-invocation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalTransactionHelper {

    private final WalletRepository walletRepository;
    private final PayoutModelRepository payoutModelRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private Wallet getWalletWithLock(Long userId) {
        return walletRepository.findByUserIdWithPessimisticLock(userId)
                .orElseThrow(() -> new RuntimeException("Wallet non trouvé (user ID: " + userId + ")"));
    }

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marquerSucces(Long payoutId, String externalId) {
        payoutModelRepository.findById(payoutId).ifPresent(p -> {
            p.setExternalPayoutId(externalId);
            p.setStatut(StatutTransaction.SUCCESS);
            p.setCompletedAt(LocalDateTime.now());
            payoutModelRepository.save(p);
        });
    }

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