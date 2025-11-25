package growzapp.backend.controller.api;

import growzapp.backend.model.dto.PayoutDTO.PayoutDTO;
import growzapp.backend.model.dto.PayoutDTO.PayoutRequest;
import growzapp.backend.model.dto.walletDTOs.DepotRequest;
import growzapp.backend.model.dto.walletDTOs.RetraitRequest;
import growzapp.backend.model.dto.walletDTOs.TransferRequest;
import growzapp.backend.model.dto.walletDTOs.WalletDTO;
import growzapp.backend.model.entite.Payout;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.PayoutRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.PayDunyaService;
import growzapp.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;
    private final PayDunyaService payDunyaService;
    private final WalletRepository walletRepository;
    private final PayoutRepository payoutRepository;


    // ==================================================================
    // =========================== DÉPÔT ================================
    // ==================================================================
    @PostMapping("/depot")
    public ResponseEntity<WalletDTO> deposer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DepotRequest request) {

        Long userId = getCurrentUserId(userDetails);
        Wallet wallet = walletService.deposerFonds(userId, request.montant());
        return ResponseEntity.ok(new WalletDTO(wallet)); // DTO propre, sans boucle
    }

    // ==================================================================
    // =========================== RETRAIT ==============================
    // ==================================================================
    @PostMapping("/retrait")
    public ResponseEntity<Transaction> demanderRetrait(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody RetraitRequest request) {

        Long userId = getCurrentUserId(userDetails);
        Transaction tx = walletService.demanderRetrait(userId, request.montant());
        return ResponseEntity.ok(tx);
    }

    // ==================================================================
    // =========================== TRANSFERT ============================
    // ==================================================================
    @PostMapping("/transfer")
    public ResponseEntity<String> transferer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody TransferRequest request) {

        Long expediteurId = getCurrentUserId(userDetails);
        walletService.transfererFonds(expediteurId, request.destinataireUserId(), request.montant());
        return ResponseEntity.ok("Transfert effectué avec succès");
    }

    // ==================================================================
    // =========================== SOLDE ================================
    // ==================================================================
    // WalletController.java → endpoint solde
    @GetMapping("/solde")
    public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        Wallet wallet = walletService.getWalletByUserId(userId);
        return ResponseEntity.ok(new WalletDTO(wallet)); // CETTE LIGNE EST BONNE
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



    // ==================================================================
    // ===================== MÉTHODE PAIEMENT VIA PAYDUNYA ==================
    // ==================================================================

    @PostMapping("/demander-payout")
    @PreAuthorize("isAuthenticated()")
public ResponseEntity<PayoutDTO> demanderPayout(
    @AuthenticationPrincipal UserDetails userDetails,
    @RequestBody PayoutRequest request) {

    Long userId = getCurrentUserId(userDetails);
    Wallet wallet = walletService.getWalletByUserId(userId);

    if (wallet.getSoldeRetirable().compareTo(request.montant()) < 0) {
        throw new IllegalArgumentException("Solde retirable insuffisant");
    }

    // Débit du solde retirable
    wallet.setSoldeRetirable(wallet.getSoldeRetirable().subtract(request.montant()));
    walletRepository.save(wallet);

    // Créer l'invoice PayDunya
    String token = payDunyaService.createInvoice(
        request.montant(), request.phone(), userDetails.getUsername(), request.type()
    );

    Payout payout = Payout.builder()
        .userId(userId)
        .userLogin(userDetails.getUsername())
        .userPhone(request.phone())
        .montant(request.montant())
        .type(request.type())
        .paydunyaToken(token)
        .paydunyaInvoiceUrl("https://app.paydunya.com/checkout/" + token)
        .build();

    payoutRepository.save(payout);

    return ResponseEntity.ok(new PayoutDTO(
        payout.getId(),
        payout.getMontant(),
        payout.getType().name(),
        payout.getStatut().name(),
        payout.getUserPhone(),
        payout.getCreatedAt(),
        payout.getPaydunyaInvoiceUrl()
    ));
}
}