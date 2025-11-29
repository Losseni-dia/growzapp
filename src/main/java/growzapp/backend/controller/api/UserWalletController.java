// src/main/java/growzapp/backend/controller/api/WalletController.java

package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.walletDTOs.DepotRequest;
import growzapp.backend.model.dto.walletDTOs.ExternalDepositRequest;
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
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.StripeDepositService;
import growzapp.backend.service.StripePayoutService;
import growzapp.backend.service.UserService;
import growzapp.backend.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.stripe.exception.StripeException;
import com.stripe.model.Payout;
import com.stripe.param.PayoutCreateParams;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

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




    // Dans UserController ou WalletController
    @GetMapping("/me/wallet")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WalletDTO> getMyWallet(Authentication auth) {
        User user = userService.getCurrentUser(auth);
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));
        return ResponseEntity.ok(new WalletDTO(wallet));
    }

    // ==================================================================
    // =========================== DÉPÔT ================================
    // ==================================================================
    // src/main/java/growzapp/backend/controller/api/WalletController.java

    @PostMapping("/deposit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createDepositSession(
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

            // LOG OBLIGATOIRE POUR VOIR SI L’URL EST BIEN GÉNÉRÉE
            log.info("Redirection Stripe générée : {}", redirectUrl);

            if (redirectUrl == null || redirectUrl.isBlank()) {
                log.error("Stripe a renvoyé une URL vide !");
                return ResponseEntity.status(500)
                        .body(Map.of("error", "Erreur interne Stripe"));
            }

            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur création session Stripe", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Impossible de contacter Stripe"));
        }
    }

    // ==================================================================
    // =========================== RETRAIT ==============================
    // ==================================================================
    // ENDPOINT DEMANDE DE RETRAIT – UTILISÉ PAR WalletPage.tsx
    @PostMapping("/demande-retrait")
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

    // ==================================================================
    // =========================== TRANSFERT ============================
    // ==================================================================
    // AJOUTE CET ENDPOINT → TRANSFERT FONCTIONNE EN 3 SECONDES

    @PostMapping("/transfer")
    public ResponseEntity<?> transferer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TransferRequest request) {

        Long expediteurId = getCurrentUserId(userDetails);

        walletService.transfererFonds(
                expediteurId,
                request.destinataireUserId(),
                request.montant(),
                request.source());

        return ResponseEntity.ok(
                java.util.Map.of("success", true, "message", "Transfert effectué avec succès"));
    }

    // ==================================================================
    // =========================== SOLDE ================================
    // ==================================================================
   @GetMapping("/solde")
   public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
       Long userId = getCurrentUserId(userDetails);
       Wallet wallet = walletService.getWalletByUserId(userId);

       // ON RENVOIE LE DTO, PAS L'ENTITÉ !
       // Dans WalletController.java
       return ResponseEntity.ok(new WalletDTO(wallet));
   }


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

       // Débit + trace
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

           Payout stripePayout = Payout.create(params);

           // Succès
           payout.setExternalPayoutId(stripePayout.getId());
           payout.setStatut(StatutTransaction.SUCCESS);
           payout.setPaydunyaStatus("paid");
           payout.setCompletedAt(LocalDateTime.now());
           payout.setPaydunyaInvoiceUrl("https://dashboard.stripe.com/test/payouts/" + stripePayout.getId());
           payoutModelRepository.save(payout);

           return ResponseEntity.ok(Map.of(
                   "success", true,
                   "message", "Retrait envoyé avec succès ! Tu recevras l’argent sous 1 à 3 jours ouvrés.",
                   "dashboardUrl", "https://dashboard.stripe.com/test/payouts/" + stripePayout.getId()));

       } catch (Exception e) {
           // Rollback automatique grâce à @Transactional
           throw new RuntimeException("Échec du virement bancaire : " + e.getMessage());
       }
   }
    



    // ==================================================================
    // ===================== MÉTHODE SÉCURISÉE COMMUNE ==================
    // ==================================================================
    private Long getCurrentUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        String login = userDetails.getUsername();

        return userRepository.findByLogin(login)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + login));
    }





}