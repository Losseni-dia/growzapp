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
     * Retrait bancaire automatique (virement SEPA)
     * Fonctionne en test + prod avec Stripe Connect
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String createBankPayoutWithNewTransaction(Long userId, BigDecimal montantEUR, String phone) {

        // 1. Vérification et mise à jour du Wallet
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        if (wallet.getSoldeRetirable().compareTo(montantEUR) < 0) {
            throw new IllegalArgumentException("Solde retirable insuffisant");
        }

        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montantEUR));
        walletRepository.saveAndFlush(wallet);

        // 2. Créer la trace en BDD - Utilisation EXPLICITE de PayoutModel
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

            // RÈGLE LA COLLISION : On utilise le nom pleinement qualifié pour Stripe
            com.stripe.model.Payout stripePayout = com.stripe.model.Payout.create(params);

            // Succès → Mise à jour de NOTRE entité PayoutModel (qui possède bien les
            // setters)
            payout.setExternalPayoutId(stripePayout.getId());
            payout.setStatut(StatutTransaction.SUCCESS);
            payout.setPaydunyaStatus(stripePayout.getStatus());
            payout.setCompletedAt(LocalDateTime.now());
            payout.setPaydunyaInvoiceUrl("https://dashboard.stripe.com/test/payouts/" + stripePayout.getId());
            payoutRepository.save(payout);

            return stripePayout.getId();

        } catch (Exception e) {
            // ROLLBACK AUTOMATIQUE grâce à @Transactional
            throw new RuntimeException("Échec du virement Stripe : " + e.getMessage());
        }
    }
}