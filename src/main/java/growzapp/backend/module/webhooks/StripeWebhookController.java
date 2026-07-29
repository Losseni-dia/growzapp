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
                case "checkout.session.expired" -> handleCheckoutExpired(event);
                case "payout.paid" -> handlePayoutPaid(event);
                case "payout.failed" -> handlePayoutFailed(event);
                case "charge.dispute.created" -> handleDisputeCreated(event);
                case "checkout.session.async_payment_failed" -> handleAsyncPaymentFailed(event);
                case "payout.canceled" -> handlePayoutCanceled(event);
                case "charge.dispute.closed" -> handleDisputeClosed(event);
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
                // ── Source de vérité : le montant FCFA d'ORIGINE saisi par l'utilisateur ──
                // (stocké en metadata lors de la création de session, voir
                // StripeDepositService)
                // On ne recalcule JAMAIS depuis le montant EUR arrondi par Stripe : cela
                // évite toute perte due au double arrondi FCFA → EUR → FCFA.
                String montantFcfaMetadata = root.path("metadata").path("montant_fcfa").asText(null);

                BigDecimal montantFCFA;
                if (montantFcfaMetadata != null && !montantFcfaMetadata.isBlank()) {
                    montantFCFA = new BigDecimal(montantFcfaMetadata);
                    log.info("DÉPÔT STRIPE → montant FCFA d'origine utilisé (depuis metadata) : {}", montantFCFA);
                } else {
                    // Fallback de sécurité si la metadata est absente (sessions créées avant ce
                    // fix)
                    BigDecimal tauxXOF = exchangeRateRepository.findByCurrencyCode("XOF")
                            .map(r -> r.getRateToBase())
                            .orElse(TAUX_XOF_PAR_EUR);
                    montantFCFA = montantEUR.multiply(tauxXOF).setScale(0, RoundingMode.HALF_UP);
                    log.warn("DÉPÔT STRIPE → metadata montant_fcfa absente, fallback reconversion : {} FCFA",
                            montantFCFA);
                }

                depositService.finaliserDepot(userId, montantFCFA, sessionId, "STRIPE_CARD");
                log.info("DÉPÔT STRIPE → user={} {}€ payés = {} FCFA crédités (montant exact, sans perte d'arrondi)",
                        userId, montantEUR, montantFCFA);
            }

        } catch (Exception ex) {
            log.error("Erreur parsing JSON webhook Stripe : {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void handleCheckoutExpired(Event event) {
        // Aucun investissement n'existe encore en base à ce stade (il n'est créé
        // qu'après succès du paiement, voir handleCheckoutCompleted) — on se
        // contente donc de logger l'abandon pour visibilité/suivi manuel.
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            String sessionId = root.path("id").asText();
            String userIdStr = root.path("client_reference_id").asText();
            String type = root.path("metadata").path("type").asText("DEPOSIT");

            log.warn("CHECKOUT EXPIRÉ (abandonné) → session={} user={} type={}", sessionId, userIdStr, type);
        } catch (Exception ex) {
            log.error("Erreur parsing JSON webhook checkout.session.expired : {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void handleDisputeCreated(Event event) {
        // Litige bancaire (chargeback). Le schéma actuel ne stocke pas l'ID du
        // payment_intent/charge sur l'investissement (seulement l'ID de session
        // Checkout), donc pas de lien automatique possible pour l'instant — on
        // logue toutes les infos utiles pour une investigation manuelle rapide.
        // TODO amélioration future : stocker paymentIntentId sur Investissement
        // pour permettre un rapprochement + gel automatique.
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            String chargeId = root.path("id").asText();
            String paymentIntentId = root.path("payment_intent").asText();
            long amount = root.path("amount").asLong(0);
            String reason = root.path("reason").asText("inconnue");

            log.error(
                    "⚠️ LITIGE STRIPE (chargeback) CRÉÉ → charge={} payment_intent={} montant={} raison={} — investigation manuelle requise",
                    chargeId, paymentIntentId, amount, reason);
        } catch (Exception ex) {
            log.error("Erreur parsing JSON webhook charge.dispute.created : {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void handleAsyncPaymentFailed(Event event) {
        // Contrepartie de checkout.session.async_payment_succeeded pour les
        // moyens de paiement asynchrones (virement différé, etc.) — comme pour
        // checkout.session.expired, aucun investissement n'a encore été créé en
        // base à ce stade (créé uniquement dans handleCheckoutCompleted), donc
        // on se limite à un log clair pour visibilité/suivi.
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            String sessionId = root.path("id").asText();
            String userIdStr = root.path("client_reference_id").asText();
            String type = root.path("metadata").path("type").asText("DEPOSIT");

            log.warn("PAIEMENT ASYNCHRONE ÉCHOUÉ → session={} user={} type={}", sessionId, userIdStr, type);
        } catch (Exception ex) {
            log.error("Erreur parsing JSON webhook checkout.session.async_payment_failed : {}", ex.getMessage(), ex);
        }
    }

    @Transactional
    public void handleDisputeClosed(Event event) {
        // Complète charge.dispute.created : indique l'issue finale du litige
        // (lost / won / warning_closed). Même limite que pour dispute.created :
        // pas de lien automatique avec l'investissement faute de payment_intent
        // stocké — log détaillé pour traitement manuel.
        String rawJson = event.getDataObjectDeserializer().getRawJson();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(rawJson);

            String chargeId = root.path("id").asText();
            String paymentIntentId = root.path("payment_intent").asText();
            String status = root.path("status").asText("inconnu");

            log.warn("⚠️ LITIGE STRIPE CLÔTURÉ → charge={} payment_intent={} issue={}",
                    chargeId, paymentIntentId, status);
        } catch (Exception ex) {
            log.error("Erreur parsing JSON webhook charge.dispute.closed : {}", ex.getMessage(), ex);
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
            Wallet wallet = walletRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Wallet introuvable pour user " + userId));

            wallet.setSoldeDisponible(wallet.getSoldeDisponible().add(montantFCFA));
            walletRepository.save(wallet);

            log.info("Wallet crédité : user={} +{} FCFA (solde disponible temporaire)", userId, montantFCFA);

            // ── 3. Appeler exactement le même flux que wallet interne ──────
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
    public void handlePayoutCanceled(Event event) {
        Payout payout = (Payout) event.getDataObjectDeserializer().getObject().orElse(null);
        if (payout == null)
            return;

        payoutModelRepository.findByExternalPayoutId(payout.getId())
                .ifPresent(p -> {
                    p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                    p.setPaydunyaStatus("canceled");
                    payoutModelRepository.save(p);
                    log.warn("RETRAIT STRIPE ANNULÉ → {}", payout.getId());
                });
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