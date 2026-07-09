package growzapp.backend.module.kyc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import growzapp.backend.module.kyc.dto.VoveIdResultDTO;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoveIdService {

    private final WebClient voveIdWebClient;

    @Value("${vove.public-key}")
    private String publicKey;

    @Value("${vove.flow-id}")
    private String flowId;

    @Value("${vove.redirect-url}")
    private String redirectUrl;

    // Générer le refId unique pour cet utilisateur
    public String generateRefId(Long userId) {
        return "KYC_USER_" + userId;
    }

    // Créer une session VOVE ID avec forceCreation dynamique
    public String createSessionToken(Long userId, boolean forceCreation) {
        String refId = generateRefId(userId);

        Map<String, Object> body = Map.of(
                "refId", refId,
                "flowId", flowId,
                "forceCreation", forceCreation);

        Map response = voveIdWebClient.post()
                .uri("/v2/sessions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return response != null ? (String) response.get("token") : null;
    }

    // Générer l'URL du widget Web SDK VOVE ID
    public String getWidgetUrl(String sessionToken) {
        return "https://web.voveid.com/" +
                "?authToken=" + sessionToken +
                "&environment=Sandbox" +
                "&publicKey=" + publicKey +
                "&lg=fr" +
                "&showUI=true" +
                "&redirectURL=" + redirectUrl;
    }

    // Récupérer le résultat depuis l'API VOVE ID avec le refId
    public VoveIdResultDTO getVerificationResult(String refId) {
        return voveIdWebClient.get()
                .uri("/v2/users/" + refId)
                .retrieve()
                .bodyToMono(VoveIdResultDTO.class)
                .block();
    }
}