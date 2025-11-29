// src/main/java/growzapp/backend/service/StripeDepositService.java
// VERSION ULTIME 2025 – DÉPÔT + INVESTISSEMENT + RETRAIT – TOUT DANS LE MÊME SERVICE

package growzapp.backend.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.Payout;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PayoutCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
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

    @Value("${app.frontend-url}") // ex: http://localhost:3000 ou https://growzapp.com
    private String frontendUrl;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    // ========================================
    // 1. DÉPÔT SUR LE WALLET (déjà existant)
    // ========================================
    public String createCheckoutSession(Long userId, double montantEUR) {
        try {
            long amountInCents = BigDecimal.valueOf(montantEUR).multiply(BigDecimal.valueOf(100)).longValueExact();

            SessionCreateParams params = SessionCreateParams.builder()
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
    // 2. INVESTISSEMENT PAR CARTE (NOUVELLE MÉTHODE)
    // ========================================
    public String createInvestissementSession(
            Long userId,
            Long projetId,
            int nombreParts,
            String projetLibelle,
            double prixUnePart) {

        try {
            double montantTotal = nombreParts * prixUnePart;
            long amountInCents = BigDecimal.valueOf(montantTotal).multiply(BigDecimal.valueOf(100)).longValueExact();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/projet/" + projetId + "?invest=success")
                    .setCancelUrl(frontendUrl + "/projet/" + projetId + "?invest=cancel")
                    .setClientReferenceId(userId.toString())
                    .putMetadata("type", "INVESTISSEMENT")
                    .putMetadata("user_id", userId.toString())
                    .putMetadata("projet_id", projetId.toString())
                    .putMetadata("nombre_parts", String.valueOf(nombreParts))
                    .putMetadata("montant", String.valueOf(montantTotal))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(amountInCents)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Investissement - " + projetLibelle)
                                                                    .setDescription(nombreParts + " part(s) à "
                                                                            + prixUnePart + "€")
                                                                    .build())
                                                    .build())
                                    .build())
                    .build();

            Session session = Session.create(params);
            log.info("Session investissement créée → {} parts → {}", nombreParts, session.getUrl());
            return session.getUrl();

        } catch (StripeException e) {
            log.error("Erreur création session investissement", e);
            throw new RuntimeException("Impossible de créer le paiement");
        }
    }

    // ========================================
    // 3. RETRAIT VERS LE PORTEUR (déjà existant)
    // ========================================
    @Transactional
    public String createBankPayoutAdmin(Long projetId, BigDecimal montantEUR) {
        {
            Projet projet = projetRepository.findById(projetId)
                    .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

            User porteur = projet.getPorteur();
            if (porteur == null || porteur.getStripeAccountId() == null) {
                throw new IllegalStateException("Porteur non connecté à Stripe");
            }

            String stripeAccountId = porteur.getStripeAccountId();

            try {
                long amountInCents = montantEUR.multiply(BigDecimal.valueOf(100)).longValueExact();

                PayoutCreateParams params = PayoutCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency("eur")
                        .setMethod(PayoutCreateParams.Method.STANDARD)
                        .putMetadata("type", "retrait_admin_projet")
                        .putMetadata("projet_id", projetId.toString())
                        .build();

                Payout payout = Payout.create(params, RequestOptions.builder()
                        .setStripeAccount(stripeAccountId)
                        .build());

                log.info("Payout admin → {} € pour le porteur {}", montantEUR, porteur.getNom(), porteur.getPrenom());
                return payout.getId();

            } catch (StripeException e) {
                throw new IllegalStateException("Échec virement Stripe : " + e.getMessage());
            }
        }
    }
}



