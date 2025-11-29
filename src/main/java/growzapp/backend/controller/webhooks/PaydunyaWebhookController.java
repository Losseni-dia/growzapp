// src/main/java/growzapp/backend/controller/webhooks/PaydunyaWebhookController.java

package growzapp.backend.controller.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.model.entite.PayoutModel;
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
import java.util.Map;

// src/main/java/growzapp/backend/controller/webhooks/PaydunyaWebhookController.java
@Slf4j
@RestController
@RequestMapping("/api/webhook/paydunya")
@RequiredArgsConstructor
public class PaydunyaWebhookController {

    @Value("${paydunya.webhook-secret}")
    private String webhookSecret;

    private final PayoutModelRepository payoutModelRepository;
    @PostMapping
    public ResponseEntity<String> handle(@RequestBody Map<String, Object> payload) {
        String token = (String) payload.get("token");
        String status = (String) payload.get("status");

        if (token == null || status == null) {
            return ResponseEntity.badRequest().body("Missing data");
        }

        payoutModelRepository.findByPaydunyaToken(token)
                .ifPresent(p -> {
                    if ("completed".equals(status) || "paid".equals(status)) {
                        p.setStatut(StatutTransaction.SUCCESS);
                        p.setPaydunyaStatus("completed");
                        p.setCompletedAt(LocalDateTime.now());
                        log.info("RETRAIT PAYDUNYA PAYÉ → {} €", p.getMontant());
                    } else {
                        p.setStatut(StatutTransaction.ECHEC_PAIEMENT);
                        p.setPaydunyaStatus(status);
                        log.warn("RETRAIT PAYDUNYA ÉCHOUÉ → {}", status);
                    }
                    payoutModelRepository.save(p);
                });

        return ResponseEntity.ok("OK");
    }

    private boolean verifySignature(Map<String, Object> payload, String signature) {
        // Logique de vérification HMAC (implémente selon la doc PayDunya)
        // Pour le test : return true;
        return true; // En prod, implémente la vérification
    }
}