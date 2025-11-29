package growzapp.backend.service;

// src/main/java/growzapp/backend/service/StripePayoutService.java

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.Stripe;
import com.stripe.model.Payout;
import com.stripe.param.PayoutCreateParams;

import growzapp.backend.model.entite.PayoutModel;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.WalletRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional // OBLIGATOIRE
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
    @Transactional( propagation = Propagation.REQUIRES_NEW)
    public String createBankPayoutWithNewTransaction(Long userId, BigDecimal montantEUR, String phone) {
        // Ton code existant, mais DANS UNE NOUVELLE TRANSACTION
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet introuvable"));

        if (wallet.getSoldeRetirable().compareTo(montantEUR) < 0) {
            throw new IllegalArgumentException("Solde retirable insuffisant");
        }

        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montantEUR));
        walletRepository.saveAndFlush(wallet);
        // 2. Créer la trace
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

            Payout stripePayout = Payout.create(params);

            // Succès → mise à jour
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