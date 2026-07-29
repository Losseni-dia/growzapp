package growzapp.backend.module.paiement.fedapay;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fedapay.model.FedaPay;
import com.fedapay.model.Payout;
import com.fedapay.model.Transaction;

import growzapp.backend.module.paiement.common.PaymentProviderService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FedaPayService implements PaymentProviderService {

    @Value("${fedapay.secret-key}")
    private String secretKey;

    @Value("${fedapay.mode:sandbox}")
    private String mode;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public String getProviderName() {
        return "FEDAPAY";
    }

    private final RestTemplate restTemplate = new RestTemplate();

    private void applyApiConfig() {
        try {
            // Le SDK FedaPay compare les chaînes avec != (bug connu du SDK,
            // comparaison de référence au lieu de .equals()) — .intern() force
            // la version canonique de la chaîne pour que la comparaison par
            // référence passe correctement.
            FedaPay.setEnvironement(mode.intern());
            FedaPay.setApiKey(secretKey.trim());
        } catch (Exception e) {
            log.error("Échec de configuration FedaPay : {}", e.getMessage());
            throw new RuntimeException("Configuration FedaPay invalide : " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentSessionResponse creerSessionDepot(BigDecimal montant, Long userId) {
        return creerSession(montant, "Dépôt sur GrowzApp",
                Map.of("type", "DEPOSIT", "user_id", userId.toString()),
                frontendUrl + "/wallet?mm_deposit=success");
    }

    @Override
    public PaymentSessionResponse creerSessionInvestissement(
            BigDecimal montant, Long userId, Long projetId, int nombreParts,
            String projetLibelle, String projetSlug) {
        return creerSession(montant,
                "Investissement — " + projetLibelle + " (" + nombreParts + " part(s))",
                Map.of(
                        "type", "INVESTISSEMENT",
                        "user_id", userId.toString(),
                        "projet_id", projetId.toString(),
                        "nombre_parts", String.valueOf(nombreParts)),
                frontendUrl + "/projet/" + projetSlug + "?mm=success");
    }

    private PaymentSessionResponse creerSession(
            BigDecimal montant, String description, Map<String, String> customMetadata, String callbackUrl) {
        applyApiConfig();
        try {
            log.info("🚀 Création transaction FedaPay : {}", description);

            Map<String, Object> params = new HashMap<>();
            params.put("description", description);
            params.put("amount", montant.intValue());
            params.put("currency", Map.of("iso", "XOF"));
            params.put("callback_url", callbackUrl);
            params.put("custom_metadata", customMetadata);

            Transaction transaction = Transaction.create(params);
            if (transaction.getId() == null) {
                throw new IllegalStateException("FedaPay: transaction sans id");
            }
            String link = transaction.generateToken().getSecurePaymentLink();
            return new PaymentSessionResponse(link, transaction.getId());

        } catch (Exception e) {
            log.error("💥 Erreur FedaPay : {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException("Échec FedaPay : " + e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // DÉCAISSEMENT — non couvert par l'implémentation MOBILI existante,
    // écrit à partir de la doc FedaPay Payout. NON TESTÉ, à valider en
    // sandbox avant toute mise en production.
    // ────────────────────────────────────────────────────────────────────
 @Override
    public PayoutResponse initierRetrait(
            BigDecimal montant, String phone, String moyenPaiement, Long referenceId) {
        applyApiConfig();
        try {
            log.info("🚀 Décaissement FedaPay : {} FCFA vers {}", montant, phone);

            Map<String, Object> params = new HashMap<>();
            params.put("amount", montant.intValue());
            params.put("currency", Map.of("iso", "XOF"));
            params.put("mode", moyenPaiement); // ex: "mtn", "moov", "orange_money"
            params.put("customer", Map.of("phone_number", Map.of("number", phone, "country", "ci")));

            Payout payout = Payout.create(params);
            if (payout.getId() == null) {
                throw new IllegalStateException("FedaPay: payout sans id");
            }

            // Le SDK Java n'expose pas d'action "start" dédiée — appel HTTP direct
            // vers PUT /v1/payouts/{id}/start, conformément à la doc FedaPay.
            String startUrl = getBaseApiUrl() + "/v1/payouts/" + payout.getId() + "/start";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + secretKey.trim());
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<Map> startResponse = restTemplate.exchange(
                    startUrl, HttpMethod.PUT, new HttpEntity<>(headers), Map.class);
            log.info("Réponse FedaPay start payout : {}", startResponse.getBody());

            return new PayoutResponse(payout.getId(), payout.getId(), "started");

        } catch (Exception e) {
            log.error("💥 Erreur décaissement FedaPay : {} - {}", e.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException("Échec décaissement FedaPay : " + e.getMessage());
        }
    }

    private String getBaseApiUrl() {
        return "sandbox".equalsIgnoreCase(mode)
                ? "https://sandbox-api.fedapay.com"
                : "https://api.fedapay.com";
    }


    public boolean isTransactionApprovedForBooking(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return false;
        }
        applyApiConfig();
        try {
            Transaction t = Transaction.retrieve(transactionId);
            if (t == null) {
                return false;
            }
            return "approved".equals(t.getStatus()) || "transferred".equals(t.getStatus());
        } catch (Exception e) {
            log.warn("FedaPay retrieve({}) : {}", transactionId, e.getMessage());
            return false;
        }
    }
}