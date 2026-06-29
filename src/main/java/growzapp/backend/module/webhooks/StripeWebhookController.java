package growzapp.backend.module.webhooks;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.Payout;
import com.stripe.net.Webhook;

import growzapp.backend.module.exchangerate.repository.ExchangeRateRepository;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.paiement.innerwallet.DepositService;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    private static final BigDecimal TAUX_XOF_PAR_EUR = new BigDecimal("655.957");

    private final PayoutModelRepository payoutModelRepository;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final InvestissementRepository investissementRepository;
    private final DepositService depositService;
    private final WalletRepository walletRepository;
    private final ExchangeRateRepository exchangeRateRepository;
    private final InvestissementService investissementService;

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

        try {
            log.info("WEBHOOK REÇU type={} id={}", event.getType(), event.getId());
            switch (event.getType()) {
                case "checkout.session.completed",
                        "checkout.session.async_payment_succeeded" ->
                    handleCheckoutCompleted(event);
                case "payout.paid" -> handlePayoutPaid(event);
                case "payout.failed" -> handlePayoutFailed(event);
                default -> log.debug("Événement Stripe ignoré : {}", event.getType());
            }
        } catch (Exception e) {
            log.error("Erreur traitement webhook {} : {}", event.getId(), e.getMessage(), e);
        }

        return ResponseEntity.ok("ok");
    }

    @Transactional
    public void handleCheckoutCompleted(Event event) {
        // getObject() peut retourner vide si la version API du SDK ne correspond pas
        // On utilise getRawJson() comme fallback
        // Parser le JSON brut directement avec Jackson (disponible dans Spring Boot)
        // getObject() échoue avec les nouvelles versions d'API Stripe
        // (2025-10-29.clover)
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        log.info("Raw JSON reçu (200 premiers chars) : {}",
                rawJson != null ? rawJson.substring(0, Math.min(200, rawJson.length())) : "null");

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            String sessionId = root.path("id").asText();
            String userIdStr = root.path("client_reference_id").asText();
            String type = root.path("metadata").path("type").asText("DEPOSIT");
            long amountTotal = root.path("amount_total").asLong(0);
            String projetIdStr = root.path("metadata").path("projet_id").asText();
            String nombrePartsStr = root.path("metadata").path("nombre_parts").asText();

            log.info("Session parsée : id={} user={} type={} amount={}", sessionId, userIdStr, type, amountTotal);

            if (userIdStr == null || userIdStr.isBlank()) {
                log.warn("client_reference_id manquant dans session {}", sessionId);
                return;
            }

            Long userId = Long.parseLong(userIdStr);
            BigDecimal montantEUR = BigDecimal.valueOf(amountTotal)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

            if ("INVESTISSEMENT".equals(type)) {
                handleInvestissementPayeRaw(sessionId, userId, montantEUR, projetIdStr, nombrePartsStr);
            } else {
                depositService.finaliserDepot(userId, montantEUR, sessionId, "STRIPE_CARD");
                log.info("DÉPÔT STRIPE → user={} +{}€", userId, montantEUR);
            }

        } catch (Exception ex) {
            log.error("Erreur parsing JSON webhook Stripe : {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void handleInvestissementPayeRaw(String sessionId, Long userId, BigDecimal montantEUR, String projetIdStr,
            String nombrePartsStr) {
        try {
            // ── Idempotence — évite double traitement ─────────────────────
            boolean dejaTraite = investissementRepository
                    .existsByReferenceExterneStripe(sessionId);
            if (dejaTraite) {
                log.warn("Session Stripe déjà traitée : {}", sessionId);
                return;
            }

            Long projetId = Long.parseLong(projetIdStr);
            int nombreParts = Integer.parseInt(nombrePartsStr);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User introuvable : " + userId));

            // ── 1. Conversion EUR → FCFA ──────────────────────────────────
            BigDecimal tauxXOF = exchangeRateRepository.findByCurrencyCode("XOF")
                    .map(r -> r.getRateToBase())
                    .orElse(TAUX_XOF_PAR_EUR);

            BigDecimal montantFCFA = montantEUR.multiply(tauxXOF)
                    .setScale(0, RoundingMode.HALF_UP);

            log.info("Stripe investissement : {}€ = {} FCFA pour user={} projet={} parts={}",
                    montantEUR, montantFCFA, userId, projetId, nombreParts);

            // ── 2. Créditer le wallet utilisateur (argent externe Stripe) ─
            // On crédite le solde disponible pour que investir() puisse le bloquer
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Wallet introuvable pour user " + userId));

            wallet.setSoldeDisponible(wallet.getSoldeDisponible().add(montantFCFA));
            walletRepository.save(wallet);

            log.info("Wallet crédité : user={} +{} FCFA (solde disponible temporaire)", userId, montantFCFA);

            // ── 3. Appeler exactement le même flux que wallet interne ──────
            // investir() va : vérifier KYC, vérifier solde, bloquer fonds,
            // créer investissement EN_ATTENTE, enregistrer transaction
            var investissementDTO = investissementService.investir(projetId, nombreParts, user);

            // ── 4. Enregistrer la référence Stripe pour idempotence ───────
            investissementRepository.findById(investissementDTO.id()).ifPresent(inv -> {
                inv.setReferenceExterneStripe(sessionId);
                investissementRepository.save(inv);
            });

            log.info("INVESTISSEMENT STRIPE EN_ATTENTE créé → id={} user={} projet={} {} FCFA",
                    investissementDTO.id(), userId, projetId, montantFCFA);

        } catch (Exception e) {
            log.error("Erreur handleInvestissementPaye session={}", sessionId, e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void handlePayoutPaid(Event event) {
        Payout payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null)
            return;

        payoutModelRepository.findByExternalPayoutId(payout.getId())
                .ifPresent(p -> {
                    p.setStatut(StatutTransaction.SUCCESS);
                    p.setPaydunyaStatus("paid");
                    p.setCompletedAt(LocalDateTime.now());
                    payoutModelRepository.save(p);
                    log.info("RETRAIT STRIPE PAYÉ → {}", payout.getId());
                });
    }

    @Transactional
    public void handlePayoutFailed(Event event) {
        Payout payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null)
            return;

        payoutModelRepository.findByExternalPayoutId(payout.getId())
                .ifPresent(p -> {
                    p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                    p.setPaydunyaStatus("failed");
                    payoutModelRepository.save(p);
                    log.warn("RETRAIT STRIPE ÉCHOUÉ → {}", payout.getId());
                });
    }
}