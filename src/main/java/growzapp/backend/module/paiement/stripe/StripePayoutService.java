package growzapp.backend.module.paiement.stripe;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.Stripe;
import com.stripe.param.PayoutCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StripePayoutService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
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