// src/main/java/growzapp/backend/controller/api/UserWalletController.java

package growzapp.backend.controller.api;

import growzapp.backend.model.dto.walletDTOs.ExternalWithdrawRequest;
import growzapp.backend.model.dto.walletDTOs.TransferRequest;
import growzapp.backend.model.dto.walletDTOs.WalletDTO;
import growzapp.backend.model.entite.PayoutModel;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.StripeDepositService;
import growzapp.backend.service.UserService;
import growzapp.backend.service.WalletService;
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
public class UserWalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PayoutModelRepository payoutModelRepository;
    private final StripeDepositService stripeDepositService;
    private final UserService userService;

    @GetMapping("/me/wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletDTO> getMyWallet(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));
        return ResponseEntity.ok(new WalletDTO(wallet));
    }

    // ==================================================================
    // ====================== DÉPÔT PAR CARTE (STRIPE) ==================
    // ==================================================================
    @PostMapping("/deposit/card")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createCardDepositSession(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montantDouble = body.get("montant");

        if (montantDouble == null || montantDouble < 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Montant minimum : 5 €"));
        }

        try {
            String redirectUrl = stripeDepositService.createCheckoutSession(userId, montantDouble);

            log.info("Redirection Stripe générée : {}", redirectUrl);

            if (redirectUrl == null || redirectUrl.isBlank()) {
                log.error("Stripe a renvoyé une URL vide !");
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Erreur interne Stripe"));
            }

            // Retourne l'URL de redirection
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur création session Stripe", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Impossible de contacter Stripe"));
        }
    }

    // ==================================================================
    // ===================== DÉPÔT PAR MOBILE MONEY (CORRIGÉ) =====================
    // ==================================================================
    @PostMapping("/deposit/mobile-money")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createMobileMoneyDeposit(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montantDouble = body.get("montant");
        BigDecimal montant = BigDecimal.valueOf(montantDouble);

        if (montantDouble == null || montantDouble < 5) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Montant minimum : 5 €"));
        }

        try {
            // CORRECTION: On récupère l'URL (String) du service.
            String redirectUrl = walletService.initierDepotMobileMoney(userId, montant);

            if (redirectUrl == null || redirectUrl.isBlank()) {
                log.error("PayDunya a renvoyé une URL vide !");
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Erreur interne PayDunya"));
            }

            // Retourne l'URL de redirection pour que le frontend redirige l'utilisateur
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur Mobile Money", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Échec de l'initialisation du dépôt Mobile Money: " + e.getMessage()));
        }
    }

    // ==================================================================
    // =========================== RETRAIT ==============================
    // ==================================================================

    // ENDPOINT DEMANDE DE RETRAIT (par admin validation)
    @PostMapping("/demande-retrait")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> demanderRetrait(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Double> body) {

        Long userId = getCurrentUserId(userDetails);
        Double montant = body.get("montant");

        if (montant == null || montant <= 0) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Montant invalide ou manquant"));
        }

        try {
            // Cette méthode crée une Transaction EN_ATTENTE_VALIDATION
            Transaction tx = walletService.demanderRetrait(userId, montant);

            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Demande de retrait envoyée avec succès !",
                    "transactionId", tx.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ENDPOINT RETRAIT DIRECT (Stripe Payout ou Mobile Money Payout)
    @PostMapping("/withdraw")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<?> withdraw(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ExternalWithdrawRequest request) {

        Long userId = getCurrentUserId(userDetails);
        BigDecimal montant = BigDecimal.valueOf(request.montant());

        // Validation
        if (montant.compareTo(BigDecimal.valueOf(5)) < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Le montant minimum est de 5 €"));
        }

        Wallet wallet = walletService.getWalletByUserId(userId);
        if (wallet.getSoldeRetirable().compareTo(montant) < 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Solde retirable insuffisant"));
        }

        // Débit du solde retirable (avant l'appel Stripe/MM)
        wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montant));
        walletRepository.save(wallet);

        // Crée le modèle de Payout pour la trace
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
            // Simuler l'appel à Stripe pour le virement
            long amountInCents = montant.multiply(BigDecimal.valueOf(100)).longValueExact();

            PayoutCreateParams params = PayoutCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .setMethod(PayoutCreateParams.Method.STANDARD)
                    .putMetadata("payout_id", payout.getId().toString())
                    .putMetadata("user_id", userId.toString())
                    .build();

            // --- SIMULATION ---
            String externalPayoutId = "po_" + System.currentTimeMillis();

            // Succès
            payout.setExternalPayoutId(externalPayoutId);
            payout.setStatut(StatutTransaction.SUCCESS);
            payout.setCompletedAt(LocalDateTime.now());
            payout.setPaydunyaInvoiceUrl("https://simulated.dashboard/payouts/" + externalPayoutId);
            payoutModelRepository.save(payout);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Retrait envoyé avec succès ! Tu recevras l’argent sous 1 à 3 jours ouvrés.",
                    "dashboardUrl", payout.getPaydunyaInvoiceUrl()));

        } catch (Exception e) {
            // Si l'appel externe échoue, on lève l'exception pour @Transactional
            log.error("Échec du virement bancaire externe", e);
            throw new RuntimeException("Échec du virement bancaire : " + e.getMessage());
        }
    }

    // ==================================================================
    // =========================== TRANSFERT ============================
    // ==================================================================
    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
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

            return ResponseEntity.ok(
                    java.util.Map.of("success", true, "message", "Transfert effectué avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ==================================================================
    // =========================== SOLDE ================================
    // ==================================================================
    @GetMapping("/solde")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(new WalletDTO(wallet));
    }

    // ==================================================================
    // ===================== MÉTHODE SÉCURISÉE COMMUNE ==================
    // ==================================================================
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        String login = userDetails.getUsername();

        return userRepository.findByLoginForAuth(login)
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + login));
    }
}