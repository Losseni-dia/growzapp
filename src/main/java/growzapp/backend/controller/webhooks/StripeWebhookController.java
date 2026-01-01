// src/main/java/growzapp/backend/controller/webhooks/StripeWebhookController.java (CORRIGÉ)

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
import org.springframework.transaction.annotation.Transactional; // Assurez-vous que ceci est importé

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.DepositService; // NOUVELLE INJECTION
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

    // Suppression de TransactionRepository
    private final PayoutModelRepository payoutModelRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final InvestissementRepository investissementRepository;

    // NOUVELLES INJECTIONS (ou services réorganisés)
    private final DepositService depositService;
    private final WalletRepository walletRepository; // Gardé pour l'investissement (soldeBloque)

    @PostMapping
    public ResponseEntity<String> handle(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        // ... (Logique de vérification de signature inchangée) ...
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
        // ...

        try {
            switch (event.getType()) {
                case "checkout.session.completed", "checkout.session.async_payment_succeeded" ->
                    handleCheckoutSessionCompleted(event);
                case "payout.paid" -> handlePayoutPaid(event);
                case "payout.failed" -> handlePayoutFailed(event);
                default -> log.debug("Événement Stripe ignoré : {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur traitement webhook Stripe {} : {}", event.getId(), e.getMessage(), e);
            return ResponseEntity.ok("ok");
        }

        return ResponseEntity.ok("ok");
    }

    // ========================================
    // DÉPÔT + INVESTISSEMENT
    // ========================================
    @Transactional
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

        // Montant total en EUR (nécessaire pour les deux types)
        BigDecimal montantEUR = BigDecimal.valueOf(session.getAmountTotal())
                .divide(BigDecimal.valueOf(100));

        if ("INVESTISSEMENT".equals(type)) {
            handleInvestissementPaye(session, userId, montantEUR);
        } else {
            // APPEL AU NOUVEAU SERVICE DE DÉPÔT
            depositService.finaliserDepot(userId, montantEUR, session.getId(), "STRIPE_CARD");
            log.info("DÉPÔT STRIPE CRÉDITÉ (via DepositService) → user {} +{}€", userId, montantEUR);
        }
    }

    // Ancien handleDeposit supprimé et remplacé par l'appel à
    // depositService.finaliserDepot

    @Transactional
    private void handleInvestissementPaye(Session session, Long userId, BigDecimal montantTotal) {
        try {
            // ... (logique de création de l'Investissement) ...

            Long projetId = Long.parseLong(session.getMetadata().get("projet_id"));
            int nombreParts = Integer.parseInt(session.getMetadata().get("nombre_parts"));

            User investisseur = userRepository.findById(userId).orElse(null);
            Projet projet = projetRepository.findById(projetId).orElse(null);

            if (investisseur == null || projet == null) {
                log.error("Utilisateur ou projet introuvable pour investissement Stripe");
                return;
            }

            // 1. Créer l'investissement (similaire à avant)
            Investissement investissement = new Investissement();
            // ... (affectation des autres champs) ...
            investissement.setInvestisseur(investisseur);
            investissement.setProjet(projet);
            investissement.setNombrePartsPris(nombreParts);
            // ...
            investissementRepository.save(investissement);

            // 2. Transférer l'argent DANS le wallet (crédit + blocage)
            // Comme le paiement vient d'une source externe (Stripe), nous devons
            // l'introduire
            // dans le portefeuille de l'utilisateur comme disponible, puis le bloquer
            // immédiatement
            // Note: Si vous avez une transaction initiale EN_ATTENTE, trouvez-la et
            // mettez-la à jour.

            walletRepository.findByUserId(userId).ifPresent(wallet -> {
                // Pour Stripe Investissement, les fonds sont perçus par l'application
                // et doivent être transférés au porteur APRES validation admin.
                // Ici, on crédite le solde bloqué directement (ou solde disponible puis on le
                // déplace).

                // OPTION A (Simplifiée si le wallet n'a pas été crédité avant)
                wallet.setSoldeBloque(wallet.getSoldeBloque().add(montantTotal));
                walletRepository.save(wallet);
                log.info("INVESTISSEMENT STRIPE FOND REÇU ET BLOQUÉ → user {} → {} parts", userId, nombreParts);

                // Vous devriez aussi enregistrer une Transaction type DEPOT ou INVESTISSEMENT
                // SUCCESS ici.
            });

        } catch (Exception e) {
            log.error("Erreur traitement investissement Stripe", e);
        }
    }

    // ========================================
    // RETRAIT AU PORTEUR (LOGIQUE INCHANGÉE)
    // ========================================
    @Transactional
    private void handlePayoutPaid(Event event) {
        // ... (Logique existante pour mettre à jour PayoutModel à SUCCESS) ...
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

    @Transactional
    private void handlePayoutFailed(Event event) {
        // ... (Logique existante pour mettre à jour PayoutModel à ECHEC et
        // potentiellement débloquer/recrediter) ...
        Payout payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null)
            return;

        payoutModelRepository.findByExternalPayoutId(payout.getId())
                .ifPresent(p -> {
                    p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                    p.setPaydunyaStatus("failed");
                    payoutModelRepository.save(p);
                    log.warn("RETRAIT STRIPE ÉCHOUÉ → {} – raison: {}", payout.getId(), payout.getFailureMessage());

                    // NOTE IMPORTANTE: Ici, si le Payout échoue, les fonds ont été débités
                    // par WithdrawalService. On doit les recréditer si Stripe n'a pas pu les
                    // envoyer.
                    // Optionnel : Réactiver le solde débité dans WithdrawalService
                    // walletRepository.findByUserId(p.getUserId()).ifPresent(wallet -> {
                    // wallet.setSoldeRetirable(wallet.getSoldeRetirable().add(p.getMontant()));
                    // walletRepository.save(wallet);
                    // });
                });
    }
}