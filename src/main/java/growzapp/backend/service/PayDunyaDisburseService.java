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
                                ? "https://app.paydunya.com/sandbox-api/v2"
                                : "https://app.paydunya.com/api/v2";
        }

        public String initiatePayout(BigDecimal montant, String phone, TypeTransaction type) {
                String url = getBaseUrl() + "/disburse/get-invoice";

                String cleanPhone = phone.replaceAll("[^0-9]", "");
                if (cleanPhone.startsWith("225"))
                        cleanPhone = "0" + cleanPhone.substring(3);
                if (cleanPhone.startsWith("221"))
                        cleanPhone = "0" + cleanPhone.substring(3);

                String withdrawMode = switch (type) {
                        case PAYOUT_OM -> "orange-money-ci";
                        case PAYOUT_MTN -> "mtn-momo-ci";
                        case PAYOUT_WAVE -> "wave-ci";
                        case PAYOUT_OM_SN -> "orange-money-senegal";
                        case PAYOUT_WAVE_SN -> "wave-sn";
                        default -> throw new IllegalArgumentException("Opérateur non supporté: " + type);
                };

                Map<String, Object> payload = Map.of(
                                "amount", montant.doubleValue(),
                                "account_alias", cleanPhone,
                                "withdraw_mode", withdrawMode,
                                "callback_url", "https://growzapp.com/api/paydunya/callback" // même en local ça passe
                );

                HttpHeaders headers = new HttpHeaders();
                headers.set("PAYDUNYA-MASTER-KEY", masterKey);
                headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
                headers.set("PAYDUNYA-TOKEN", token);
                headers.setContentType(MediaType.APPLICATION_JSON);

                try {
                        ResponseEntity<Map> response = restTemplate.postForEntity(
                                        url, new HttpEntity<>(payload, headers), Map.class);

                        Map<String, Object> body = response.getBody();
                        if (body == null || !"success".equals(body.get("status"))) {
                                String error = body != null ? body.toString() : "réponse vide";
                                throw new RuntimeException("PayDunya refus: " + error);
                        }

                        Map<String, Object> data = (Map<String, Object>) body.get("data");
                        return (String) data.get("invoice_url");

                } catch (Exception e) {
                        throw new RuntimeException("Erreur PayDunya Disburse: " + e.getMessage(), e);
                }
        }
}