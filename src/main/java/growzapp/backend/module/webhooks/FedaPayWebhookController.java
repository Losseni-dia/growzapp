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
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
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
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final InvestissementService investissementService;

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

                Wallet wallet = walletRepository.findByUserId(userId)
                        .orElseThrow(() -> new RuntimeException("Wallet introuvable : " + userId));
                wallet.setSoldeDisponible(wallet.getSoldeDisponible().add(montant));
                walletRepository.save(wallet);

                investissementService.investir(projetId, nombreParts, user);
                log.info("INVESTISSEMENT FEDAPAY EN_ATTENTE → user={} projet={} parts={} montant={}",
                        userId, projetId, nombreParts, montant);
            } else {
                walletService.deposerFonds(userId, montant.doubleValue(), "FEDAPAY_MM");
                log.info("DÉPÔT FEDAPAY CRÉDITÉ → user={} montant={}", userId, montant);
            }

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