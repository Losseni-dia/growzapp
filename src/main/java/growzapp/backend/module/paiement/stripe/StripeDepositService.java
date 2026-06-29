// src/main/java/growzapp/backend/module/paiement/stripe/StripeDepositService.java
package growzapp.backend.module.paiement.stripe;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeDepositService {

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // ── 1. DÉPÔT SUR LE WALLET (Carte Bancaire) ──────────────────────────────
    public String createCheckoutSession(Long userId, double montantEUR) {
        try {
            long amountInCents = BigDecimal.valueOf(montantEUR)
                    .multiply(BigDecimal.valueOf(100)).longValueExact();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setLocale(SessionCreateParams.Locale.FR)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/wallet?deposit=success")
                    .setCancelUrl(frontendUrl + "/wallet?deposit=cancel")
                    .setClientReferenceId(userId.toString())
                    .putMetadata("type", "DEPOSIT")
                    .putMetadata("user_id", userId.toString())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Dépôt sur GrowzApp")
                                                                    .setDescription(
                                                                            "Crédit de ton portefeuille GrowzApp")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Erreur Stripe dépôt wallet", e);
            throw new RuntimeException("Erreur Stripe Checkout dépôt: " + e.getMessage());
        }
    }

    // ── 2. INVESTISSEMENT DIRECT PAR CARTE ───────────────────────────────────
    // Note: Le montant Stripe est en EUR (cents). Le FCFA est converti côté
    // backend.
    // Taux indicatif : 1 EUR = 655.957 FCFA (taux fixe XOF/EUR)
    public String createInvestissementSession(
            Long userId,
            Long projetId,
            String projetSlug,
            int nombreParts,
            String projetLibelle,
            BigDecimal prixUnePartFCFA) {
        try {
            // Conversion FCFA → EUR (taux fixe BCEAO)
            BigDecimal tauxFCFAparEUR = BigDecimal.valueOf(655.957);
            BigDecimal montantTotalFCFA = prixUnePartFCFA.multiply(BigDecimal.valueOf(nombreParts));
            BigDecimal montantEUR = montantTotalFCFA.divide(tauxFCFAparEUR, 2, java.math.RoundingMode.HALF_UP);
            long amountInCents = montantEUR.multiply(BigDecimal.valueOf(100)).longValue();

            // Minimum Stripe : 50 centimes EUR
            if (amountInCents < 50) {
                throw new RuntimeException("Montant trop faible pour Stripe (minimum 0.50 €)");
            }

            SessionCreateParams params = SessionCreateParams.builder()
                    .setLocale(SessionCreateParams.Locale.FR)
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/projet/" + projetSlug + "?stripe=success")
                    .setCancelUrl(frontendUrl + "/projet/" + projetSlug + "?stripe=cancel")
                    .setClientReferenceId(userId.toString())
                    .putMetadata("type", "INVESTISSEMENT")
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("projet_id", projetId.toString())
                    .putMetadata("nombre_parts", String.valueOf(nombreParts))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity((long) nombreParts)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(montantEUR.multiply(BigDecimal.valueOf(100))
                                                            .divide(BigDecimal.valueOf(nombreParts), 0,
                                                                    java.math.RoundingMode.HALF_UP)
                                                            .longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Part — " + projetLibelle)
                                                                    .setDescription(nombreParts + " part(s) × "
                                                                            + montantTotalFCFA.toPlainString()
                                                                            + " FCFA")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            log.info("Session Stripe investissement créée : {} pour user={} projet={} parts={}",
                    session.getId(), userId, projetId, nombreParts);
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Erreur création session investissement Stripe", e);
            throw new RuntimeException("Impossible de créer le paiement Stripe: " + e.getMessage());
        }
    }
}