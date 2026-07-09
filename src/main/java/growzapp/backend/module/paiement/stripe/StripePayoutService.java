package growzapp.backend.module.paiement.stripe;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.Stripe;
import com.stripe.param.PayoutCreateParams;

import growzapp.backend.module.paiement.model.PayoutModel;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StripePayoutService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    private final PayoutModelRepository payoutRepository;
    private final WalletRepository walletRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * Retrait bancaire — méthode historique (gère elle-même le wallet via
     * soldeRetirable).
     * Conservée pour compatibilité avec retirerDuProjetWallet().
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createBankPayoutWithNewTransaction(Long userId, BigDecimal montantEUR, String phone) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        if (wallet.getSoldeRetirable().compareTo(montantEUR) < 0) {
            throw new IllegalArgumentException("Solde retirable insuffisant");
        }

        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montantEUR));
        walletRepository.saveAndFlush(wallet);

        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .montant(montantEUR)
                .userPhone(phone)
                .type(TypeTransaction.PAYOUT_STRIPE)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .build();
        payout = payoutRepository.save(payout);

        try {
            long amountInCents = montantEUR.multiply(BigDecimal.valueOf(100)).longValueExact();

            PayoutCreateParams params = PayoutCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setMethod(PayoutCreateParams.Method.STANDARD)
                    .putMetadata("payout_id", payout.getId().toString())
                    .putMetadata("user_id", userId.toString())
                    .build();

            com.stripe.model.Payout stripePayout = com.stripe.model.Payout.create(params);

            payout.setExternalPayoutId(stripePayout.getId());
            payout.setStatut(StatutTransaction.SUCCESS);
            payout.setPaydunyaStatus(stripePayout.getStatus());
            payout.setCompletedAt(LocalDateTime.now());
            payout.setPaydunyaInvoiceUrl("https://dashboard.stripe.com/test/payouts/" + stripePayout.getId());
            payoutRepository.save(payout);

            return stripePayout.getId();

        } catch (Exception e) {
            throw new RuntimeException("Échec du virement Stripe : " + e.getMessage());
        }
    }

    /**
     * Payout Stripe direct — NE touche PAS au wallet (déjà débité en amont
     * par WithdrawalService.executerRetraitBancaire() sur soldeDisponible).
     */
    public String createBankPayoutDirect(Long userId, BigDecimal montantEUR, Long payoutId) {
        try {
            long amountInCents = montantEUR.multiply(BigDecimal.valueOf(100)).longValueExact();

            PayoutCreateParams params = PayoutCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setMethod(PayoutCreateParams.Method.STANDARD)
                    .putMetadata("payout_id", payoutId.toString())
                    .putMetadata("user_id", userId.toString())
                    .build();

            com.stripe.model.Payout stripePayout = com.stripe.model.Payout.create(params);
            return stripePayout.getId();

        } catch (Exception e) {
            throw new RuntimeException("Échec du virement Stripe : " + e.getMessage(), e);
        }
    }
}