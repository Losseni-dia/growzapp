// src/main/java/growzapp/backend/service/StripeDepositService.java

package growzapp.backend.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeDepositService {

    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;

    @Value("${stripe.secret-key}")
    private String stripeSecretKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // ========================================
    // 1. DÉPÔT SUR LE WALLET (Carte Bancaire)
    // ========================================
    public String createCheckoutSession(Long userId, double montantEUR) {
        try {
            long amountInCents = BigDecimal.valueOf(montantEUR).multiply(BigDecimal.valueOf(100)).longValueExact();

            // CONVERSION POUR FIXER L'ERREUR 67108979
            // Le SDK Stripe attend un objet SessionCreateParams.Locale,
            // que nous créons ici à partir de la chaîne "fr" en majuscule.
            SessionCreateParams.Locale locale = SessionCreateParams.Locale.valueOf("FR");

            SessionCreateParams params = SessionCreateParams.builder()
                    .setLocale(locale) // Utilise l'énumération convertie
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
                                                                    .setDescription("Crédit de ton portefeuille")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (StripeException e) {
            throw new RuntimeException("Erreur Stripe Checkout dépôt: " + e.getMessage());
        }
    }

    // ========================================
    // 2. INVESTISSEMENT PAR CARTE
    // ========================================
    public String createInvestissementSession(
            Long userId,
            Long projetId,
            int nombreParts,
            String projetLibelle,
            BigDecimal prixUnePart) {
        try {
            BigDecimal montantTotal = BigDecimal.valueOf(nombreParts).multiply(prixUnePart);
            long amountInCents = montantTotal.multiply(BigDecimal.valueOf(100)).longValueExact();

            // NOTE: Ajoutez .setLocale(locale) ici aussi si vous utilisez l'investissement
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setLocale(SessionCreateParams.Locale.valueOf("FR"))
                    // ... (reste des paramètres) ...
                    .build();

            Session session = Session.create(params);
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Erreur création session investissement", e);
            throw new RuntimeException("Impossible de créer le paiement");
        }
    }

    // 3. createBankPayoutAdmin DOIT ÊTRE DANS STRIPEPAYOUTSERVICE
}