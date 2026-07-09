package growzapp.backend.module.kyc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import growzapp.backend.module.kyc.dto.VoveIdResultDTO;
import growzapp.backend.module.kyc.dto.VoveIdSessionDTO;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.kyc.service.KycVoveIdService;
import growzapp.backend.module.kyc.service.VoveIdService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;

import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC — VOVE ID", description = "Endpoints de vérification d'identité automatique via VOVE ID")
public class KycVoveIdController {

    private final VoveIdService voveIdService;
    private final UserRepository userRepository;
    private final KycVoveIdService kycVoveIdService;

    @Value("${vove.public-key}")
    private String publicKey;

    @Operation(summary = "Démarrer la vérification KYC via VOVE ID", description = "Retourne le refId et l'URL du widget VOVE ID. "
            +
            "forceCreation est automatiquement true si le statut est NON_SOUMIS ou REJETE.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Widget URL retournée avec succès"),
            @ApiResponse(responseCode = "400", description = "KYC déjà validé — aucune action nécessaire"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    @PostMapping("/start-voveid")
    public ResponseEntity<?> startVoveId(
            @AuthenticationPrincipal UserDetails userDetails) {

        User user = userRepository.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Bloquer si déjà validé
        if (user.getKycStatus() == KycStatus.VALIDE) {
            return ResponseEntity.badRequest()
                    .body("Votre identité est déjà vérifiée et validée.");
        }

        // forceCreation = true si NON_SOUMIS ou REJETE
        // forceCreation = false si EN_ATTENTE (reprend la session existante)
        boolean forceCreation = user.getKycStatus() == KycStatus.NON_SOUMIS
                || user.getKycStatus() == KycStatus.REJETE;

        String refId = voveIdService.generateRefId(user.getId());
        String sessionToken = voveIdService.createSessionToken(user.getId(), forceCreation);
        String widgetUrl = voveIdService.getWidgetUrl(sessionToken);

        return ResponseEntity.ok(new VoveIdSessionDTO(refId, widgetUrl, publicKey));
    }

    @Operation(summary = "Webhook VOVE ID — Réception des notifications", description = "Endpoint appelé automatiquement par VOVE ID. Ne pas appeler manuellement.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Webhook traité avec succès"),
            @ApiResponse(responseCode = "500", description = "Erreur traitement webhook")
    })
    @PostMapping("/webhook/voveid")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody Map<String, Object> payload) {

        String refId = (String) payload.get("refId");
        System.out.println("Webhook VOVE ID recu — refId: " + refId);

        if (refId != null && refId.startsWith("KYC_USER_")) {
            Long userId = Long.parseLong(refId.replace("KYC_USER_", ""));
            VoveIdResultDTO result = voveIdService.getVerificationResult(refId);
            kycVoveIdService.updateKycStatusFromVoveId(userId, result);
        }

        return ResponseEntity.ok().build();
    }
}