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

// PayDunyaService.java – VERSION QUI MARCHE À 1000000 %
@Service
@RequiredArgsConstructor
public class PayDunyaService {

        @Value("${paydunya.master-key}")
        private String masterKey;

        @Value("${paydunya.private-key}")
        private String privateKey;

        @Value("${paydunya.token}")
        private String token;

        private final RestTemplate restTemplate = new RestTemplate();

        public String createInvoice(BigDecimal montant, String phone, String name, TypeTransaction type) {
                String url = "https://app.paydunya.com/api/v1/checkout-invoice/create";

                Map<String, Object> payload = Map.of(
                                "invoice", Map.of(
                                                "total_amount", montant.doubleValue(),
                                                "description", "Retrait GrowzApp - " + name),
                                "store", Map.of(
                                                "name", "GrowzApp",
                                                "website_url", "https://growzapp.com",
                                                "logo_url", "https://growzapp.com/logo.png"),
                                "actions", Map.of(
                                                "mobile_money", true,
                                                "orange_money_sn", true,
                                                "wave_sn", true),
                                "custom_data", Map.of(
                                                "user_phone", phone,
                                                "type", type.name()));

                HttpHeaders headers = new HttpHeaders();
                headers.set("PAYDUNYA-MASTER-KEY", masterKey);
                headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
                headers.set("PAYDUNYA-TOKEN", token);
                headers.setContentType(MediaType.APPLICATION_JSON);

                try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        url, new HttpEntity<>(payload, headers), Map.class);

                        Map<String, Object> body = response.getBody();
                        if (body == null || body.get("response") == null) {
                                throw new RuntimeException("PayDunya: réponse invalide ou vide");
                        }

                        Map<String, Object> responseMap = (Map<String, Object>) body.get("response");
                        String token = (String) responseMap.get("token");
                        if (token == null || token.isBlank()) {
                                throw new RuntimeException("PayDunya: token manquant");
                        }

                        return token;

                } catch (Exception e) {
                        throw new RuntimeException("Erreur PayDunya: " + e.getMessage());
                }
        }
}