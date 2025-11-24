package growzapp.backend.controller.api;

import growzapp.backend.model.dto.walletDTOs.DepotRequest;
import growzapp.backend.model.dto.walletDTOs.RetraitRequest;
import growzapp.backend.model.dto.walletDTOs.TransferRequest;
import growzapp.backend.model.dto.walletDTOs.WalletDTO;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    // ==================================================================
    // =========================== DÉPÔT ================================
    // ==================================================================
    @PostMapping("/depot")
    public ResponseEntity<Wallet> deposer(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody DepotRequest request) {

        Long userId = getCurrentUserId(userDetails);
        Wallet wallet = walletService.deposerFonds(userId, request.montant());
        return ResponseEntity.ok(wallet);
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
    @GetMapping("/solde")
    public ResponseEntity<WalletDTO> getSolde(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getCurrentUserId(userDetails);
        var wallet = walletService.getWalletByUserId(userId);
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

        return userRepository.findByLogin(login)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable : " + login));
    }
}