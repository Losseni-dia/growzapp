package growzapp.backend.service;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import growzapp.backend.model.enumeration.TypeTransaction;
import lombok.RequiredArgsConstructor;


// src/main/java/growzapp/backend/service/PayDunyaDisburseService.java
@Service
@RequiredArgsConstructor
public class PayDunyaDisburseService {

        @Value("${paydunya.master-key}")
        private String masterKey;

        @Value("${paydunya.private-key}")
        private String privateKey;

        @Value("${paydunya.token}")
        private String token;

        @Value("${paydunya.mode:test}")
        private String mode;

        private final RestTemplate restTemplate = new RestTemplate();

        private String getBaseUrl() {
                return "test".equalsIgnoreCase(mode)
                                ? "https://app.paydunya.com/sandbox-api/v1"
                                : "https://app.paydunya.com/api/v1";
        }

        public String initiatePayout(BigDecimal montant, String phone, TypeTransaction type) {
                String url = getBaseUrl() + "/checkout-invoice/create"; // ENDPOINT ACTUEL 2025

                // Format téléphone : +225XXXXXXXX ou 0XXXXXXXX
                String cleanPhone = phone.replaceAll("\\D", "");
                if (cleanPhone.startsWith("225") && cleanPhone.length() == 11) {
                        cleanPhone = "0" + cleanPhone.substring(3); // 22507... → 007...
                }

                String withdrawMode = switch (type) {
                        case PAYOUT_OM -> "orange-money-ci";
                        case PAYOUT_MTN -> "mtn-momo-ci";
                        case PAYOUT_WAVE -> "wave-ci";
                        case PAYOUT_MOOV -> "moov-money-ci";
                        default -> throw new IllegalArgumentException("Opérateur non supporté: " + type);
                };

                Map<String, Object> payload = Map.of(
                                "invoice", Map.of(
                                                "total_amount", montant.doubleValue(),
                                                "description", "Retrait GrowzApp"),
                                "store", Map.of(
                                                "name", "GrowzApp",
                                                "website_url", "https://growzapp.com"),
                                "actions", Map.of(
                                                "mobile_money", true),
                                "custom_data", Map.of(
                                                "phone_number", cleanPhone,
                                                "withdraw_mode", withdrawMode));

                HttpHeaders headers = new HttpHeaders();
                headers.set("PAYDUNYA-MASTER-KEY", masterKey);
                headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
                headers.set("PAYDUNYA-TOKEN", token);
                headers.setContentType(MediaType.APPLICATION_JSON);

                try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        url, new HttpEntity<>(payload, headers), Map.class);

                        Map<String, Object> body = response.getBody();
                        if (body == null || !"success".equals(body.get("response_text"))) {
                                String error = body != null ? body.toString() : "réponse vide";
                                throw new RuntimeException("PayDunya refus: " + error);
                        }

                        Map<String, Object> responseMap = (Map<String, Object>) body.get("response");
                        String invoiceUrl = (String) responseMap.get("invoice_url");

                        if (invoiceUrl == null || invoiceUrl.isBlank()) {
                                throw new RuntimeException("URL de paiement manquante");
                        }

                        return invoiceUrl;

                } catch (Exception e) {
                        throw new RuntimeException("Erreur PayDunya: " + e.getMessage(), e);
                }
        }
}