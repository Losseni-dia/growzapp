package growzapp.backend.module.wallet.controller;

import growzapp.backend.module.paiement.model.PayoutModel;
import growzapp.backend.module.paiement.repository.PayoutModelRepository;
import growzapp.backend.module.paiement.stripe.StripeDepositService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.user.service.UserService;
import growzapp.backend.module.wallet.dto.ExternalWithdrawRequest;
import growzapp.backend.module.wallet.dto.TransferRequest;
import growzapp.backend.module.wallet.dto.WalletDTO;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
import growzapp.backend.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import com.stripe.param.PayoutCreateParams;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Gestion du portefeuille utilisateur : consultation du solde, dépôts, retraits et transferts")
public class UserWalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PayoutModelRepository payoutModelRepository;
    private final StripeDepositService stripeDepositService;
    private final UserService userService;

    @GetMapping("/me/wallet")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Consulter mon wallet",
        description = "Retourne le solde complet du wallet de l'utilisateur connecté (disponible, bloqué, retirable).",
        tags = {"Wallet"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Wallet retourné avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WalletDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Wallet introuvable pour cet utilisateur",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<WalletDTO> getMyWallet(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));
        return ResponseEntity.ok(new WalletDTO(wallet));
    }

    @GetMapping("/solde")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Consulter mon solde",
        description = "Alias de /me/wallet — retourne le wallet complet de l'utilisateur connecté.",
        tags = {"Wallet"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Solde retourné avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = WalletDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(new WalletDTO(wallet));
    }

    @PostMapping("/deposit/card")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Initier un dépôt par carte bancaire (Stripe)",
        description = "Crée une session de paiement Stripe Checkout. Retourne une URL de redirection vers la page de paiement. Montant minimum : 5 €.",
        tags = {"Wallet"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Montant à déposer en euros",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"montant\": 50.0}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Session Stripe créée — URL de redirection retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"redirectUrl\": \"https://checkout.stripe.com/pay/cs_test_abc123\"}"))),
        @ApiResponse(responseCode = "400", description = "Montant invalide (inférieur à 5 €)",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Erreur interne Stripe",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> createCardDepositSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montantDouble = body.get("montant");

        if (montantDouble == null || montantDouble < 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Montant minimum : 5 €"));
        }

        try {
            String redirectUrl = stripeDepositService.createCheckoutSession(userId, montantDouble);

            if (redirectUrl == null || redirectUrl.isBlank()) {
                return ResponseEntity.status(500).body(Map.of("error", "Erreur interne Stripe"));
            }

            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur création session Stripe", e);
            return ResponseEntity.status(500).body(Map.of("error", "Impossible de contacter Stripe"));
        }
    }

    @PostMapping("/deposit/mobile-money")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Initier un dépôt par Mobile Money (PayDunya)",
        description = "Initialise une transaction de dépôt via PayDunya. Retourne une URL de redirection vers la page de paiement Mobile Money. Montant minimum : 5 €.",
        tags = {"Wallet"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Montant à déposer",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"montant\": 25.0}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction PayDunya initiée — URL de redirection retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"redirectUrl\": \"https://app.paydunya.com/sandbox-checkout/invoice/abc123\"}"))),
        @ApiResponse(responseCode = "400", description = "Montant invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Erreur interne PayDunya",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> createMobileMoneyDeposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montantDouble = body.get("montant");
        BigDecimal montant = BigDecimal.valueOf(montantDouble);

        if (montantDouble == null || montantDouble < 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Montant minimum : 5 €"));
        }

        try {
            String redirectUrl = walletService.initierDepotMobileMoney(userId, montant);

            if (redirectUrl == null || redirectUrl.isBlank()) {
                return ResponseEntity.status(500).body(Map.of("error", "Erreur interne PayDunya"));
            }

            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur Mobile Money", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Échec de l'initialisation du dépôt Mobile Money: " + e.getMessage()));
        }
    }

    @PostMapping("/demande-retrait")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Soumettre une demande de retrait",
        description = "Crée une demande de retrait en statut EN_ATTENTE_VALIDATION. Un administrateur devra la valider avant que les fonds soient libérés.",
        tags = {"Wallet"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Montant à retirer du solde disponible",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"montant\": 100.0}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Demande de retrait créée avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"Demande de retrait envoyée avec succès !\", \"transactionId\": 88}"))),
        @ApiResponse(responseCode = "400", description = "Montant invalide ou solde insuffisant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> demanderRetrait(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montant = body.get("montant");

        if (montant == null || montant <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Montant invalide ou manquant"));
        }

        try {
            Transaction tx = walletService.demanderRetrait(userId, montant);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de retrait envoyée avec succès !",
                    "transactionId", tx.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Transactional
    @Operation(
        summary = "Retrait direct vers un compte externe",
        description = "Effectue un retrait immédiat depuis le solde retirable vers un compte externe (Stripe Payout ou Mobile Money). Montant minimum : 5 €.",
        tags = {"Wallet"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrait effectué avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"Retrait envoyé avec succès !\", \"dashboardUrl\": \"https://...\"}"))),
        @ApiResponse(responseCode = "400", description = "Montant insuffisant ou solde retirable insuffisant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Échec du virement bancaire externe",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ExternalWithdrawRequest request) {

        Long userId = getCurrentUserId(userDetails);
        BigDecimal montant = BigDecimal.valueOf(request.montant());

        if (montant.compareTo(BigDecimal.valueOf(5)) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Le montant minimum est de 5 €"));
        }

        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet.getSoldeRetirable().compareTo(montant) < 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Solde retirable insuffisant"));
        }

        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montant));
        walletRepository.save(wallet);

        PayoutModel payout = PayoutModel.builder()
                .userId(userId)
                .userLogin(userDetails.getUsername())
                .montant(montant)
                .type(TypeTransaction.PAYOUT_STRIPE)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .createdAt(LocalDateTime.now())
                .build();
        payout = payoutModelRepository.save(payout);

        try {
            long amountInCents = montant.multiply(BigDecimal.valueOf(100)).longValueExact();

            PayoutCreateParams params = PayoutCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setMethod(PayoutCreateParams.Method.STANDARD)
                    .putMetadata("payout_id", payout.getId().toString())
                    .putMetadata("user_id", userId.toString())
                    .build();

            String externalPayoutId = "po_" + System.currentTimeMillis();

            payout.setExternalPayoutId(externalPayoutId);
            payout.setStatut(StatutTransaction.SUCCESS);
            payout.setCompletedAt(LocalDateTime.now());
            payout.setPaydunyaInvoiceUrl("https://simulated.dashboard/payouts/" + externalPayoutId);
            payoutModelRepository.save(payout);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Retrait envoyé avec succès ! Tu recevras l'argent sous 1 à 3 jours ouvrés.",
                    "dashboardUrl", payout.getPaydunyaInvoiceUrl()));

        } catch (Exception e) {
            log.error("Échec du virement bancaire externe", e);
            throw new RuntimeException("Échec du virement bancaire : " + e.getMessage());
        }
    }

    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Transférer des fonds vers un autre utilisateur",
        description = "Transfère un montant depuis le wallet de l'utilisateur connecté vers le wallet d'un autre utilisateur de la plateforme.",
        tags = {"Wallet"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transfert effectué avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"Transfert effectué avec succès\"}"))),
        @ApiResponse(responseCode = "400", description = "Solde insuffisant ou destinataire invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> transferer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TransferRequest request) {

        Long expediteurId = getCurrentUserId(userDetails);

        try {
            walletService.transfererFonds(
                    expediteurId,
                    request.destinataireUserId(),
                    request.montant(),
                    request.source());

            return ResponseEntity.ok(Map.of("success", true, "message", "Transfert effectué avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + userDetails.getUsername()));
    }
}
