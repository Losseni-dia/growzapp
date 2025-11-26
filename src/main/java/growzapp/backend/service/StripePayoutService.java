// src/main/java/growzapp/backend/service/StripePayoutService.java
package growzapp.backend.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Payout; // <-- Classe Stripe
import com.stripe.param.PayoutCreateParams;
import growzapp.backend.model.entite.PayoutModel; // <-- TON entité JPA
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StripePayoutService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    private final PayoutModelRepository payoutRepository;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public String createPayoutDirect(BigDecimal montantEUR, String phone, TypeTransaction type) throws StripeException {

        long amountInCents = montantEUR.multiply(BigDecimal.valueOf(100)).longValueExact();

        PayoutCreateParams params = PayoutCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("eur") // EUR pour les tests bancaires Stripe
                .setMethod(PayoutCreateParams.Method.STANDARD)
                .putMetadata("user_phone", phone != null ? phone : "N/A")
                .putMetadata("app_payout_type", type.name())
                .build();

        Payout stripePayout = Payout.create(params);

        PayoutModel appPayout = PayoutModel.builder()
                .montant(montantEUR)
                .userPhone(phone)
                .type(type)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .externalPayoutId(stripePayout.getId()) // ← bon champ
                .paydunyaInvoiceUrl("https://dashboard.stripe.com/test/payouts/" + stripePayout.getId())
                .paydunyaStatus(stripePayout.getStatus())
                .createdAt(LocalDateTime.now())
                .build();

        payoutRepository.save(appPayout);

        return stripePayout.getId();
    }
}