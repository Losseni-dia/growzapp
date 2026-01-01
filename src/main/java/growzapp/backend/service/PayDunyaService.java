// src/main/java/growzapp/backend/service/PayDunyaService.java

package growzapp.backend.service;

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

import growzapp.backend.model.enumeration.TypeTransaction;
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

        // ==================================================================
        // 1. DÉPÔT MOBILE MONEY (COLLECTE)
        // ==================================================================
        public PayDunyaResponse createDepositCheckoutSession(BigDecimal montant, Long userId) {
                String url = getBaseUrl() + "/checkout-invoice/create";

                HttpHeaders headers = new HttpHeaders();
                headers.set("PAYDUNYA-MASTER-KEY", appMasterKey);
                headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
                headers.set("PAYDUNYA-TOKEN", token);
                headers.setContentType(MediaType.APPLICATION_JSON);

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
                                                "user_id", userId));

                try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        url, new HttpEntity<>(payload, headers), Map.class);

                        Map<String, Object> body = response.getBody();

                        // Log pour le débogage (à conserver pour l'instant)
                        log.info("Réponse complète PayDunya: {}", body);

                        if (body == null || !"00".equals(body.get("response_code"))) {
                                String errorDetails = body != null ? body.toString() : "réponse vide";
                                log.error("PayDunya échec: {}", errorDetails);
                                throw new RuntimeException("Échec PayDunya: " + errorDetails);
                        }

                        // Si le code est '00', le service a réussi
                        // 🟢 CORRECTION DÉFINITIVE: Utiliser la clé 'response_text' comme l'URL
                        // l'indique.
                        String invoiceUrl = (String) body.get("response_text");
                        String invoiceToken = (String) body.get("token");

                        log.info("PayDunya URL de redirection générée: {}", invoiceUrl);

                        if (invoiceUrl == null || invoiceUrl.isBlank() || invoiceToken == null) {
                                throw new RuntimeException("URL/Token de paiement PayDunya manquante");
                        }

                        // Retourne l'objet PayDunyaResponse avec la bonne URL
                        return new PayDunyaResponse(invoiceUrl, invoiceToken);

                } catch (HttpClientErrorException e) {
                        log.error("Erreur HTTP PayDunya : {} - Corps: {}", e.getStatusCode(),
                                        e.getResponseBodyAsString());
                        throw new RuntimeException("Erreur de communication PayDunya.", e);
                } catch (Exception e) {
                        log.error("Erreur lors de la création de la session PayDunya", e);
                        throw new RuntimeException("Impossible de créer la session de dépôt.", e);
                }
        }

        // ==================================================================
        // 2. RETRAIT MOBILE MONEY (DISBURSEMENT / PAYOUT)
        // ==================================================================
        public String initiatePayout(BigDecimal montant, String phone, TypeTransaction type, Long payoutId) {
                log.warn("INFO: Utilisation d'un endpoint de Payout simulé/simplifié.");
                return "PD-" + type.name() + "-" + payoutId;
        }

        // Classe interne simple pour retourner les deux valeurs (URL et Token)
        public record PayDunyaResponse(String redirectUrl, String invoiceToken) {
        }
}