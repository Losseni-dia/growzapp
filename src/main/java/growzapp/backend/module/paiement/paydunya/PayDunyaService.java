package growzapp.backend.module.paiement.paydunya;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import growzapp.backend.module.wallet.enums.TypeTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayDunyaService {

        @Value("${paydunya.app-master-key}")
        private String appMasterKey;

        @Value("${paydunya.private-key}")
        private String privateKey;

        @Value("${paydunya.token}")
        private String token;

        @Value("${paydunya.mode:test}")
        private String mode;

        @Value("${app.frontend-url}")
        private String frontendUrl;

        private final RestTemplate restTemplate = new RestTemplate();

        private String getBaseUrl() {
                return "test".equalsIgnoreCase(mode)
                                ? "https://app.paydunya.com/sandbox-api/v1"
                                : "https://app.paydunya.com/api/v1";
        }

        private HttpHeaders buildHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.set("PAYDUNYA-MASTER-KEY", appMasterKey);
                headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
                headers.set("PAYDUNYA-TOKEN", token);
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
        }

        private PayDunyaResponse parseResponse(Map<String, Object> body, String context) {
                if (body == null || !"00".equals(body.get("response_code"))) {
                        String err = body != null ? body.toString() : "réponse vide";
                        log.error("PayDunya échec [{}] : {}", context, err);
                        throw new RuntimeException("Échec PayDunya [" + context + "] : " + err);
                }
                String invoiceUrl = (String) body.get("response_text");
                String invoiceToken = (String) body.get("token");
                if (invoiceUrl == null || invoiceUrl.isBlank() || invoiceToken == null) {
                        throw new RuntimeException("URL/Token PayDunya manquant");
                }
                log.info("PayDunya session créée [{}] : token={}", context, invoiceToken);
                return new PayDunyaResponse(invoiceUrl, invoiceToken);
        }

        // ── 1. DÉPÔT SUR LE WALLET (Mobile Money) ────────────────────────────────
        public PayDunyaResponse createDepositCheckoutSession(BigDecimal montant, Long userId) {
                String url = getBaseUrl() + "/checkout-invoice/create";

                Map<String, Object> payload = Map.of(
                                "invoice", Map.of(
                                                "total_amount", montant.doubleValue(),
                                                "description", "Dépôt sur GrowzApp"),
                                "store", Map.of(
                                                "name", "GrowzApp",
                                                "website_url", "https://growzapp.com"),
                                "actions", Map.of(
                                                "cancel_url", frontendUrl + "/wallet?mm_deposit=cancel",
                                                "return_url", frontendUrl + "/wallet?mm_deposit=success"),
                                "custom_data", Map.of(
                                                "type", "DEPOSIT",
                                                "user_id", userId.toString()));

                try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        url, new HttpEntity<>(payload, buildHeaders()), Map.class);
                        log.info("Réponse PayDunya dépôt : {}", response.getBody());
                        return parseResponse(response.getBody(), "DEPOT");
                } catch (HttpClientErrorException e) {
                        log.error("Erreur HTTP PayDunya dépôt : {}", e.getResponseBodyAsString());
                        throw new RuntimeException("Erreur PayDunya.", e);
                }
        }

        // ── 2. INVESTISSEMENT PAR MOBILE MONEY ───────────────────────────────────
        public PayDunyaResponse createInvestissementSession(
                        BigDecimal montantFCFA,
                        Long userId,
                        Long projetId,
                        int nombreParts,
                        String projetLibelle,
                        String projetSlug) {

                String url = getBaseUrl() + "/checkout-invoice/create";

                Map<String, Object> payload = Map.of(
                                "invoice", Map.of(
                                                "total_amount", montantFCFA.doubleValue(),
                                                "description", "Investissement — " + projetLibelle
                                                                + " (" + nombreParts + " part(s))"),
                                "store", Map.of(
                                                "name", "GrowzApp",
                                                "website_url", "https://growzapp.com"),
                                "actions", Map.of(
                                                "cancel_url", frontendUrl + "/projet/" + projetSlug + "?mm=cancel",
                                                "return_url", frontendUrl + "/projet/" + projetSlug + "?mm=success"),
                                "custom_data", Map.of(
                                                "type", "INVESTISSEMENT",
                                                "user_id", userId.toString(),
                                                "projet_id", projetId.toString(),
                                                "nombre_parts", String.valueOf(nombreParts)));

                try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        url, new HttpEntity<>(payload, buildHeaders()), Map.class);
                        log.info("Réponse PayDunya investissement : {}", response.getBody());
                        return parseResponse(response.getBody(), "INVESTISSEMENT");
                } catch (HttpClientErrorException e) {
                        log.error("Erreur HTTP PayDunya investissement : {}", e.getResponseBodyAsString());
                        throw new RuntimeException("Erreur PayDunya.", e);
                }
        }

        // ── 3. RETRAIT MOBILE MONEY (simulé) ─────────────────────────────────────
        public String initiatePayout(BigDecimal montant, String phone, TypeTransaction type, Long payoutId) {
                log.warn("Payout PayDunya simulé.");
                return "PD-" + type.name() + "-" + payoutId;
        }

        public record PayDunyaResponse(String redirectUrl, String invoiceToken) {
        }
}