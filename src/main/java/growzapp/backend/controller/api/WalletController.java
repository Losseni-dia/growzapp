package growzapp.backend.controller.api;

import com.stripe.exception.StripeException;
import com.stripe.model.Payout;
import com.stripe.model.checkout.Session;
import com.stripe.param.PayoutCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import growzapp.backend.model.dto.walletDTOs.ExternalDepositRequest;
import growzapp.backend.model.dto.walletDTOs.ExternalWithdrawRequest;
import growzapp.backend.model.dto.walletDTOs.TransferRequest;
import growzapp.backend.model.dto.walletDTOs.WalletDTO;
import growzapp.backend.model.entite.PayoutModel;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.repository.PayoutModelRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

        private final WalletService walletService;
        private final UserRepository userRepository;
        private final WalletRepository walletRepository;
        private final PayoutModelRepository payoutRepository;
        private final TransactionRepository transactionRepository;

        // ==================================================================
        // DÉPÔT EXTERNE
        // ==================================================================
        @PostMapping("/deposit")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<?> deposit(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody ExternalDepositRequest request) throws StripeException {

                Long userId = getCurrentUserId(userDetails);
                BigDecimal montant = BigDecimal.valueOf(request.montant());
                Wallet wallet = walletService.getWalletByUserId(userId);

                Transaction tx = Transaction.builder()
                                .wallet(wallet)
                                .montant(montant)
                                .type(TypeTransaction.DEPOT)
                                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                                .description("Dépôt via " + request.method())
                                .build();
                tx = transactionRepository.save(tx);

                if ("STRIPE_CARD".equals(request.method())) {
                        // La clé API est déjà définie dans StripePayoutService ou via @PostConstruct →
                        // PLUS BESOIN ICI
                        SessionCreateParams params = SessionCreateParams.builder()
                                        .setMode(SessionCreateParams.Mode.PAYMENT)
                                        .setSuccessUrl("http://localhost:3000/deposit/success?session_id={CHECKOUT_SESSION_ID}")
                                        .setCancelUrl("http://localhost:3000/deposit/cancel")
                                        .addLineItem(SessionCreateParams.LineItem.builder()
                                                        .setQuantity(1L)
                                                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                                                        .setCurrency("xof")
                                                                        .setUnitAmount(montant.longValue() * 100)
                                                                        .setProductData(SessionCreateParams.LineItem.PriceData.ProductData
                                                                                        .builder()
                                                                                        .setName("Dépôt GrowzApp")
                                                                                        .build())
                                                                        .build())
                                                        .build())
                                        .setClientReferenceId(String.valueOf(tx.getId()))
                                        .putMetadata("transaction_id", String.valueOf(tx.getId()))
                                        .build();

                        Session session = Session.create(params);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "redirectUrl", session.getUrl()));
                }

                // PayDunya / autres
                String redirectUrl = switch (request.method()) {
                        case "ORANGE_MONEY" -> "https://app.paydunya.com/checkout/orange-money?invoice=" + tx.getId();
                        case "WAVE" -> "https://pay.wave.com/pay?ref=" + tx.getId();
                        case "MTN_MOMO" -> "https://momo.mtn.ci/pay?ref=" + tx.getId();
                        default -> throw new IllegalArgumentException("Méthode non supportée");
                };

                return ResponseEntity.ok(Map.of("success", true, "redirectUrl", redirectUrl));
        }

        // ==================================================================
        // RETRAIT EXTERNE
        // ==================================================================
        @PostMapping("/withdraw")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<?> withdraw(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody ExternalWithdrawRequest request) throws StripeException {

                Long userId = getCurrentUserId(userDetails);
                BigDecimal montant = BigDecimal.valueOf(request.montant());

                if (montant.compareTo(BigDecimal.valueOf(5)) < 0) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Montant minimum : 5 €"));
                }

                Wallet wallet = walletService.getWalletByUserId(userId);
                if (wallet.getSoldeRetirable().compareTo(montant) < 0) {
                        return ResponseEntity.badRequest().body(Map.of("message", "Solde retirable insuffisant"));
                }

                // 1. Débit immédiat du solde retirable
                wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(montant));
                walletRepository.save(wallet);

                // 2. Création du payout en base
                PayoutModel payout = PayoutModel.builder()
                                .userId(userId)
                                .userLogin(userDetails.getUsername())
                                .userPhone(request.phone())
                                .montant(montant)
                                .type(request.type() != null ? request.type() : TypeTransaction.RETRAIT)
                                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                                .createdAt(LocalDateTime.now())
                                .build();
                payout = payoutRepository.save(payout);

                // 3. Traitement selon la méthode
                if ("BANK_TRANSFER".equals(request.method())) {
                        long amountInCents = montant.multiply(BigDecimal.valueOf(100)).longValueExact();

                        // Payout DIRECT depuis ton compte platform → fonctionne dès que tu as un compte
                        // bancaire EUR dans le dashboard
                        PayoutCreateParams params = PayoutCreateParams.builder()
                                        .setAmount(amountInCents)
                                        .setCurrency("eur")
                                        .setMethod(PayoutCreateParams.Method.STANDARD)
                                        .putMetadata("payout_id", String.valueOf(payout.getId()))
                                        .putMetadata("user_login", userDetails.getUsername())
                                        .build();

                        Payout stripePayout = Payout.create(params);

                        payout.setExternalPayoutId(stripePayout.getId());
                        payout.setPaydunyaInvoiceUrl(
                                        "https://dashboard.stripe.com/test/payouts/" + stripePayout.getId());
                        payout.setPaydunyaStatus(stripePayout.getStatus());
                        payout.setType(TypeTransaction.PAYOUT_STRIPE);
                        payoutRepository.save(payout);

                        return ResponseEntity.ok(Map.of(
                                        "success", true,
                                        "message", "Retrait bancaire envoyé ! Reçu sous 1-2 jours ouvrés",
                                        "payoutId", stripePayout.getId(),
                                        "dashboardUrl",
                                        "https://dashboard.stripe.com/test/payouts/" + stripePayout.getId()));
                } else {
                        // Mobile Money → PayDunya
                        String redirectUrl = switch (request.method()) {
                                case "ORANGE_MONEY" -> "https://app.paydunya.com/sandbox-disburse/orange-money?token="
                                                + payout.getId();
                                case "WAVE" -> "https://app.paydunya.com/sandbox-disburse/wave?token=" + payout.getId();
                                case "MTN_MOMO" ->
                                        "https://app.paydunya.com/sandbox-disburse/mtn-momo?token=" + payout.getId();
                                default -> throw new IllegalArgumentException("Méthode non supportée");
                        };

                        return ResponseEntity.ok(Map.of("success", true, "redirectUrl", redirectUrl));
                }
        }

        // ==================================================================
        // SOLDE
        // ==================================================================
        @GetMapping("/solde")
        public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
                Long userId = getCurrentUserId(userDetails);
                Wallet wallet = walletService.getWalletByUserId(userId);
                return ResponseEntity.ok(new WalletDTO(wallet));
        }


        // ==================================================================
        // DEMANDE DE RETRAIT (depuis le portefeuille — validation admin requise)
        // ==================================================================
        // WalletController.java → DEMANDE DE RETRAIT (parfaite)
        @PostMapping("/demande-retrait")
        @PreAuthorize("isAuthenticated()")
        @Transactional
        public ResponseEntity<?> demanderRetrait(
                        @AuthenticationPrincipal UserDetails userDetails,
                        @RequestBody Map<String, Double> body) {

                Double montantDouble = body.get("montant");
                if (montantDouble == null || montantDouble < 5.0) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Montant minimum : 5 €"));
                }

                BigDecimal montant = BigDecimal.valueOf(montantDouble);
                Long userId = getCurrentUserId(userDetails);

                Wallet wallet = walletService.getWalletByUserId(userId);

                if (wallet.getSoldeDisponible().compareTo(montant) < 0) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "Solde disponible insuffisant"));
                }

                // 1. Bloquer les fonds
                wallet.bloquerFonds(montant);
                walletRepository.save(wallet);

                // 2. Créer la transaction visible dans l’historique
                Transaction demande = Transaction.builder()
                                .wallet(wallet)
                                .montant(montant)
                                .type(TypeTransaction.RETRAIT)
                                .statut(StatutTransaction.EN_ATTENTE_VALIDATION)
                                .description("Demande de retrait en attente de validation admin")
                                .createdAt(LocalDateTime.now())
                                .build();

                transactionRepository.save(demande);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Demande envoyée ! En attente de validation admin.",
                                "demandeId", demande.getId()));
        }

        // ==================================================================
        // TRANSFERT P2P de growzapp
                // ==================================================================
        @PostMapping("/transfer")
        @PreAuthorize("isAuthenticated()")
        @Transactional
        public ResponseEntity<?> transferer(
        @AuthenticationPrincipal UserDetails userDetails,
        @RequestBody TransferRequest request) {

        Long expediteurId = getCurrentUserId(userDetails);
        BigDecimal montant = BigDecimal.valueOf(request.montant());

        if (expediteurId.equals(request.destinataireUserId())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Tu ne peux pas te transférer de l'argent à toi-même"));
        }

        Wallet walletExp = walletService.getWalletByUserId(expediteurId);
        Wallet walletDest = walletService.getWalletByUserId(request.destinataireUserId());

        // === CHOIX EXPLICITE DE LA SOURCE ===
        boolean fromRetirable = "RETIRABLE".equals(request.source());

        if (fromRetirable) {
                // Transfert depuis le solde retirable
                if (walletExp.getSoldeRetirable().compareTo(montant) < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Solde retirable insuffisant"));
                }

                walletExp.setSoldeRetirable(walletExp.getSoldeRetirable().subtract(montant));
                walletDest.setSoldeRetirable(walletDest.getSoldeRetirable().add(montant));

                Transaction txOut = Transaction.builder()
                        .wallet(walletExp)
                        .destinataireWallet(walletDest)
                        .montant(montant)
                        .type(TypeTransaction.TRANSFER_OUT)
                        .statut(StatutTransaction.SUCCESS)
                        .description("Transfert de gains validés → @" + walletDest.getUser().getLogin())
                        .completedAt(LocalDateTime.now())
                        .build();

                Transaction txIn = Transaction.builder()
                        .wallet(walletDest)
                        .destinataireWallet(walletExp)
                        .montant(montant)
                        .type(TypeTransaction.TRANSFER_IN)
                        .statut(StatutTransaction.SUCCESS)
                        .description("Réception de gains validés")
                        .completedAt(LocalDateTime.now())
                        .build();

                transactionRepository.save(txOut);
                transactionRepository.save(txIn);
                walletRepository.save(walletExp);
                walletRepository.save(walletDest);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Transfert de gains validés envoyé instantanément !"
                ));
        } else {
                // Transfert depuis le solde disponible
                if (walletExp.getSoldeDisponible().compareTo(montant) < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Solde disponible insuffisant"));
                }

                walletService.transfererFonds(expediteurId, request.destinataireUserId(), request.montant());

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Transfert envoyé avec succès !"
                ));
        }
        }

        // ==================================================================
        // UTILITAIRE
        // ==================================================================
        private Long getCurrentUserId(UserDetails userDetails) {
                if (userDetails == null)
                        throw new IllegalStateException("Utilisateur non authentifié");
                String login = userDetails.getUsername();
                return userRepository.findByLogin(login)
                                .map(User::getId)
                                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + login));
        }
}