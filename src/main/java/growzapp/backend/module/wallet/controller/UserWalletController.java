package growzapp.backend.module.wallet.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.paiement.stripe.StripeDepositService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.user.service.UserService;
import growzapp.backend.module.wallet.dto.TransferRequest;
import growzapp.backend.module.wallet.dto.WalletDTO;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet", description = "Gestion du portefeuille utilisateur : consultation du solde, dépôts, retraits et transferts")
public class UserWalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final StripeDepositService stripeDepositService;
    private final UserService userService;

    @GetMapping("/me/wallet")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Consulter mon wallet", description = "Retourne le solde complet du wallet de l'utilisateur connecté (disponible, bloqué, retirable).", tags = {
            "Wallet" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Wallet retourné avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Wallet introuvable pour cet utilisateur", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
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
    @Operation(summary = "Consulter mon solde", description = "Alias de /me/wallet — retourne le wallet complet de l'utilisateur connecté.", tags = {
            "Wallet" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Solde retourné avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(new WalletDTO(wallet));
    }

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Historique des transactions de mon wallet", description = "Retourne toutes les transactions (dépôts, retraits, transferts, investissements) du wallet de l'utilisateur connecté, triées du plus récent au plus ancien.", tags = {
            "Wallet" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Historique des transactions", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Transaction.class)))
    })
    public ResponseEntity<List<Transaction>> getMyTransactions(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        return ResponseEntity.ok(walletService.getHistoriqueTransactions(userId));
    }

    @PostMapping("/deposit/card")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Initier un dépôt par carte bancaire (Stripe)", description = "Crée une session de paiement Stripe Checkout. Retourne une URL de redirection vers la page de paiement. Montant minimum : 5 €.", tags = {
            "Wallet" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Montant à déposer en euros", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"montant\": 50.0}"))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Session Stripe créée — URL de redirection retournée", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"redirectUrl\": \"https://checkout.stripe.com/pay/cs_test_abc123\"}"))),
            @ApiResponse(responseCode = "400", description = "Montant invalide (inférieur à 5 €)", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne Stripe", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
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
    @Operation(summary = "Initier un dépôt par Mobile Money (PayDunya)", description = "Initialise une transaction de dépôt via PayDunya. Retourne une URL de redirection vers la page de paiement Mobile Money. Montant minimum : 5 €.", tags = {
            "Wallet" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Montant à déposer", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"montant\": 25.0}"))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction PayDunya initiée — URL de redirection retournée", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"redirectUrl\": \"https://app.paydunya.com/sandbox-checkout/invoice/abc123\"}"))),
            @ApiResponse(responseCode = "400", description = "Montant invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "500", description = "Erreur interne PayDunya", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> createMobileMoneyDeposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montantDouble = body.get("montant");

        if (montantDouble == null || montantDouble < 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Montant minimum : 5 €"));
        }

        BigDecimal montant = BigDecimal.valueOf(montantDouble);

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

    // ── RETRAIT AUTOMATIQUE (sans validation admin) ─────────────────────────
    @PostMapping("/retrait")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Retirer des fonds (automatique, sans validation admin)", description = "Vérifie le solde disponible, débite immédiatement, puis exécute le virement réel "
            +
            "via Stripe (carte/banque) ou PayDunya (Mobile Money). " +
            "En cas d'échec du décaissement, les fonds sont automatiquement remboursés sur le wallet GrowzApp.", tags = {
                    "Wallet" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Montant et méthode de retrait", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"montant\": 50000, \"methode\": \"MOBILE_MONEY\", \"phone\": \"0700000000\"}"))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrait traité (succès ou échec avec remboursement)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Montant invalide ou solde insuffisant", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> retirerFonds(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Object> body) {

        Long userId = getCurrentUserId(userDetails);

        Object montantObj = body.get("montant");
        if (montantObj == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Champ 'montant' requis"));
        }

        BigDecimal montant = new BigDecimal(montantObj.toString());
        String methode = (String) body.getOrDefault("methode", "MOBILE_MONEY");
        String phone = (String) body.get("phone");
        // Clé d'idempotence générée côté frontend au moment du clic — protège
        // contre le double-clic (HIGH-06). Fallback UUID serveur si absente,
        // pour ne pas casser un ancien client pas encore mis à jour.
        String idempotencyKey = (String) body.get("idempotencyKey");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = java.util.UUID.randomUUID().toString();
        }
        if (montant.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Montant invalide"));
        }
        try {
            String externalId = walletService.retirerFonds(userId, montant, methode, phone, idempotencyKey);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Retrait effectué avec succès !",
                    "referenceExterne", externalId));

        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "errorCode", "WITHDRAWAL_INVALID",
                    "error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur retrait — détail technique : {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "errorCode", "WITHDRAWAL_FAILED_REFUNDED"));
        }
    }

    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Transférer des fonds vers un autre utilisateur", description = "Transfère un montant depuis le wallet de l'utilisateur connecté vers le wallet d'un autre utilisateur de la plateforme.", tags = {
            "Wallet" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transfert effectué avec succès", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"success\": true, \"message\": \"Transfert effectué avec succès\"}"))),
            @ApiResponse(responseCode = "400", description = "Solde insuffisant ou destinataire invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
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
                .orElseThrow(
                        () -> new UsernameNotFoundException("Utilisateur introuvable : " + userDetails.getUsername()));
    }
}