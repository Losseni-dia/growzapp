package growzapp.backend.module.webhooks;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/webhook/fedapay")
@RequiredArgsConstructor
public class FedaPayWebhookController {

    @Value("${fedapay.webhook-secret}")
    private String webhookSecret;

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final InvestissementService investissementService;
    private final TransactionRepository transactionRepository;
    private final growzapp.backend.module.projet.service.ProjetService projetService;

    @PostMapping
    @Transactional
    public ResponseEntity<Void> handle(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Webhook-Secret", required = false) String secret) {

        if (!secureEquals(webhookSecret, secret)) {
            log.error("❌ Webhook FedaPay — secret incorrect !");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Map<String, Object> entity = asStringObjectMap(payload.get("entity"));
            if (entity == null) {
                return ResponseEntity.ok().build();
            }

            String status = (String) entity.get("status");
            log.info("WEBHOOK FEDAPAY reçu : status={}", status);

            if (!"approved".equals(status) && !"transferred".equals(status)) {
                log.debug("Événement FedaPay ignoré (status={})", status);
                return ResponseEntity.ok().build();
            }

            Map<String, Object> metadata = asStringObjectMap(entity.get("custom_metadata"));
            if (metadata == null) {
                log.warn("Webhook FedaPay sans custom_metadata, ignoré");
                return ResponseEntity.ok().build();
            }

            // ── IDEMPOTENCE ──────────────────────────────────────────────────
            // FedaPay envoie plusieurs événements pour un même paiement
            // (ex: "approved" PUIS "transferred"), tous deux acceptés par le
            // filtre ci-dessus — sans ce garde-fou, chaque événement relançait
            // investissementService.investir(), créant un second
            // investissement/blocage de fonds pour un seul paiement réel.
            // La transaction "EN_ATTENTE_PAIEMENT" créée à l'initiation du
            // paiement (referenceExterne = id FedaPay) sert de verrou : un
            // événement qui la trouve déjà consommée est un rejeu, ignoré.
            String fedapayId = String.valueOf(entity.get("id"));
            java.util.Optional<Transaction> initiationOpt = transactionRepository.findByReferenceExterne(fedapayId);
            if (initiationOpt.isPresent() && initiationOpt.get().getStatut() != StatutTransaction.EN_ATTENTE_PAIEMENT) {
                log.info("Webhook FedaPay id={} déjà traité — événement rejoué ignoré (status={})", fedapayId, status);
                return ResponseEntity.ok().build();
            }

            String type = String.valueOf(metadata.getOrDefault("type", "DEPOSIT"));
            String userIdStr = String.valueOf(metadata.get("user_id"));
            Object amountObj = entity.get("amount");
            BigDecimal montant = new BigDecimal(String.valueOf(amountObj));

            if (userIdStr == null || "null".equals(userIdStr)) {
                log.warn("Webhook FedaPay sans user_id dans custom_metadata");
                return ResponseEntity.ok().build();
            }
            Long userId = Long.parseLong(userIdStr);

            if ("INVESTISSEMENT".equals(type)) {
                Long projetId = Long.parseLong(String.valueOf(metadata.get("projet_id")));
                int nombreParts = Integer.parseInt(String.valueOf(metadata.getOrDefault("nombre_parts", "1")));

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("User introuvable : " + userId));

                // L'argent vient de FedaPay, jamais du wallet interne : crédite
                // directement soldeBloque (pas de passage artificiel par
                // soldeDisponible, qui faussait le solde affiché — cf
                // Wallet.crediterDirectementBloque).
                investissementService.investirDepuisPaiementExterne(projetId, nombreParts, user,
                        growzapp.backend.module.wallet.enums.SourcePaiement.MOBILE_MONEY);
                log.info("INVESTISSEMENT FEDAPAY EN_ATTENTE → user={} projet={} parts={} montant={}",
                        userId, projetId, nombreParts, montant);
            } else if ("PREMIUM".equals(type)) {
                Long projetId = Long.parseLong(String.valueOf(metadata.get("projet_id")));
                projetService.activerPremiumExterne(projetId,
                        growzapp.backend.module.wallet.enums.SourcePaiement.MOBILE_MONEY);
                log.info("PREMIUM FEDAPAY ACTIVÉ → projet={} user={}", projetId, userId);
            } else {
                walletService.deposerFonds(userId, montant.doubleValue(), "FEDAPAY_MM");
                log.info("DÉPÔT FEDAPAY CRÉDITÉ → user={} montant={}", userId, montant);
            }

            // Consomme la transaction d'initiation : supprime l'entrée
            // "en attente de paiement" orpheline qui restait affichée
            // indéfiniment dans l'historique du wallet à côté de
            // l'investissement réel, et verrouille l'idempotence pour tout
            // événement FedaPay ultérieur portant le même id.
            initiationOpt.ifPresent(transactionRepository::delete);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("💥 Erreur Webhook FedaPay : {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean secureEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }

    private Map<String, Object> asStringObjectMap(Object source) {
        if (!(source instanceof Map<?, ?> rawMap)) {
            return null;
        }
        // Collectors.toMap() plante avec un NullPointerException dès qu'une
        // valeur de la Map est null (piège classique Java) — la réponse
        // FedaPay contient de nombreux champs null (canceled_at, mode, etc.),
        // donc on construit la Map manuellement pour bien les tolérer.
        Map<String, Object> result = new java.util.HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
    }
}