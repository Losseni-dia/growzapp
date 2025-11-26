package growzapp.backend.controller.webhooks;



import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/webhook/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private final TransactionRepository transactionRepository;
    private final PayoutModelRepository payoutModelRepository;
    private final WalletRepository walletRepository;

    @PostMapping
    public ResponseEntity<String> handle(@RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Signature Stripe invalide : {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        switch (event.getType()) {

            // DÉPÔT STRIPE – Checkout Session
            case "checkout.session.completed" -> {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session == null) {
                    log.warn("Session Stripe nulle dans checkout.session.completed");
                    return ResponseEntity.ok("ok");
                }

                String txId = session.getMetadata().get("transaction_id");
                if (txId == null) {
                    txId = session.getClientReferenceId();
                }

                if (txId != null && txId != null) {
                    transactionRepository.findById(Long.parseLong(txId))
                            .ifPresent(tx -> {
                                tx.setStatut(StatutTransaction.SUCCESS);
                                tx.setCompletedAt(LocalDateTime.now());
                                tx.setDescription("Dépôt Stripe confirmé – " + session.getId());

                                // Crédit du wallet
                                tx.getWallet().crediterDisponible(tx.getMontant());
                                walletRepository.save(tx.getWallet());

                                transactionRepository.save(tx);
                                log.info("DÉPÔT STRIPE VALIDÉ → transaction {} (+{} XOF)", tx.getId(), tx.getMontant());
                            });
                } else {
                    log.warn("Aucun transaction_id trouvé dans la session Stripe {}", session.getId());
                }
            }

            // RETRAIT STRIPE – Payout
            case "payout.paid" -> {
                Payout stripePayout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
                if (stripePayout == null) {
                    log.warn("Payout Stripe nul dans payout.paid");
                    return ResponseEntity.ok("ok");
                }

                // CHANGEMENT ICI : on utilise le bon nom de méthode
                payoutModelRepository.findByExternalPayoutId(stripePayout.getId())
                        .ifPresent(p -> {
                            p.setStatut(StatutTransaction.SUCCESS);
                            p.setPaydunyaStatus("paid");
                            p.setCompletedAt(LocalDateTime.now());
                            payoutModelRepository.save(p);
                            log.info("RETRAIT STRIPE PAYÉ → payout {} ({} XOF)", stripePayout.getId(), p.getMontant());
                        });
            }

            default -> log.debug("Événement Stripe ignoré : {}", event.getType());
        }

        return ResponseEntity.ok("ok");
    }
}