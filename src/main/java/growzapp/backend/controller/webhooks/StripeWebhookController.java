// src/main/java/growzapp/backend/controller/webhooks/StripeWebhookController.java
// VERSION FINALE 2025 – TOUT EST GÉRÉ : DÉPÔT, INVESTISSEMENT, RETRAIT

package growzapp.backend.controller.webhooks;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final PayoutModelRepository payoutModelRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final InvestissementRepository investissementRepository;

    @PostMapping
    public ResponseEntity<String> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Webhook Stripe - Signature invalide : {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook Stripe - Erreur parsing : {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid payload");
        }

        String eventId = event.getId();
        log.info("Webhook Stripe reçu : {} (type: {})", eventId, event.getType());

        try {
            switch (event.getType()) {

                // DÉPÔT CLASSIQUE
                case "checkout.session.completed" -> handleCheckoutSessionCompleted(event);
                case "checkout.session.async_payment_succeeded" -> handleCheckoutSessionCompleted(event);

                // RETRAIT AU PORTEUR
                case "payout.paid" -> handlePayoutPaid(event);
                case "payout.failed" -> handlePayoutFailed(event);

                default -> log.debug("Événement Stripe ignoré : {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur traitement webhook Stripe {} : {}", eventId, e.getMessage(), e);
            return ResponseEntity.ok("ok"); // Ne jamais renvoyer 500
        }

        return ResponseEntity.ok("ok");
    }

    // ========================================
    // DÉPÔT + INVESTISSEMENT
    // ========================================
    private void handleCheckoutSessionCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
        if (session == null)
            return;

        String userIdStr = session.getClientReferenceId();
        if (userIdStr == null || userIdStr.isBlank()) {
            log.warn("client_reference_id manquant dans session {}", session.getId());
            return;
        }

        Long userId = Long.parseLong(userIdStr);
        String type = session.getMetadata().get("type");

        if ("INVESTISSEMENT".equals(type)) {
            handleInvestissementPaye(session, userId);
        } else {
            handleDeposit(session, userId);
        }
    }

    private void handleDeposit(Session session, Long userId) {
        BigDecimal montant = BigDecimal.valueOf(session.getAmountTotal())
                .divide(BigDecimal.valueOf(100));

        walletRepository.findByUserId(userId).ifPresent(wallet -> {
            wallet.crediterDisponible(montant);
            walletRepository.save(wallet);
            log.info("DÉPÔT STRIPE CRÉDITÉ → user {} +{}€ (session: {})", userId, montant, session.getId());
        });
    }

    private void handleInvestissementPaye(Session session, Long userId) {
        try {
            Long projetId = Long.parseLong(session.getMetadata().get("projet_id"));
            int nombreParts = Integer.parseInt(session.getMetadata().get("nombre_parts"));
            BigDecimal montantTotal = BigDecimal.valueOf(Double.parseDouble(session.getMetadata().get("montant")));

            User investisseur = userRepository.findById(userId).orElse(null);
            Projet projet = projetRepository.findById(projetId).orElse(null);

            if (investisseur == null || projet == null) {
                log.error("Utilisateur ou projet introuvable pour investissement Stripe");
                return;
            }

            // Vérifier disponibilité des parts
            if (projet.getPartsDisponible() - projet.getPartsPrises() < nombreParts) {
                log.error("Plus assez de parts disponibles pour l'investissement");
                return;
            }

            // Créer l'investissement
            Investissement investissement = new Investissement();
            investissement.setInvestisseur(investisseur);
            investissement.setProjet(projet);
            investissement.setNombrePartsPris(nombreParts);
            investissement.setDate(LocalDateTime.now());
            investissement.setMontantInvesti(montantTotal);
            investissement.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
            investissementRepository.save(investissement);

            // Bloquer les fonds dans le wallet
            Wallet wallet = walletRepository.findByUserId(userId).orElseThrow();
            wallet.setSoldeBloque(wallet.getSoldeBloque().add(montantTotal));
            walletRepository.save(wallet);

            log.info("INVESTISSEMENT STRIPE CONFIRMÉ → user {} → {} parts → projet {}",
                    userId, nombreParts, projetId);

        } catch (Exception e) {
            log.error("Erreur traitement investissement Stripe", e);
        }
    }

    // ========================================
    // RETRAIT AU PORTEUR
    // ========================================
    private void handlePayoutPaid(Event event) {
        Payout payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null)
            return;

        payoutModelRepository.findByExternalPayoutId(payout.getId())
                .ifPresent(p -> {
                    p.setStatut(StatutTransaction.SUCCESS);
                    p.setPaydunyaStatus("paid");
                    p.setCompletedAt(LocalDateTime.now());
                    payoutModelRepository.save(p);
                    log.info("RETRAIT STRIPE PAYÉ → {} ({}€)", payout.getId(), p.getMontant());
                });
    }

    private void handlePayoutFailed(Event event) {
        Payout payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null) return;

        payoutModelRepository.findByExternalPayoutId(payout.getId())
                .ifPresent(p -> {
                    p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                    p.setPaydunyaStatus("failed");
                    payoutModelRepository.save(p);
                    log.warn("RETRAIT STRIPE ÉCHOUÉ → {} – raison: {}", payout.getId(), payout.getFailureMessage());
                });
    }
}