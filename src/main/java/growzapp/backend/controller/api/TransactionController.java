// src/main/java/growzapp/backend/controller/api/TransactionController.java
// VERSION FINALE ULTIME – wallet_id + wallet_type (27 NOV 2025)

package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.walletDTOs.RejetRetraitRequest;
import growzapp.backend.model.dto.walletDTOs.TransactionDTO;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.*;
import growzapp.backend.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final DtoConverter dtoConverter;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
   

    // ==================================================================
    // 1. Historique personnel (WalletPage) – USER seulement
    // ==================================================================
    // 1. Historique personnel
    @GetMapping("/mes-transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(
                    @AuthenticationPrincipal UserDetails userDetails) {

            Long userId = extractUserId(userDetails);

            // Récupère le wallet de l'utilisateur
            Wallet wallet = walletRepository.findByUserId(userId)
                            .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));

            // Utilise la nouvelle méthode avec EntityGraph
            List<Transaction> transactions = transactionRepository
                            .findByWalletTypeAndWalletIdOrderByCreatedAtDesc(wallet.getId());

            List<TransactionDTO> dtos = transactions.stream()
                            .map(dtoConverter::toTransactionDto)
                            .toList();

            return ResponseEntity.ok(dtos);
    }

    // 2. Retraits en attente
    @GetMapping("/retraits-en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransactionDTO>> getPendingWithdrawals() {
        List<Transaction> transactions = transactionRepository
                .findByTypeAndStatutAndWalletType(
                        TypeTransaction.RETRAIT,
                        StatutTransaction.EN_ATTENTE_VALIDATION);

        List<TransactionDTO> dtos = transactions.stream()
                .map(dtoConverter::toTransactionDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ==================================================================
    // 3. Admin : Valider un retrait (USER)
    // ==================================================================
    @PatchMapping("/{id}/valider-retrait")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> validerRetrait(@PathVariable Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de retrait introuvable"));

        if (tx.getType() != TypeTransaction.RETRAIT || tx.getWalletType() != WalletType.USER) {
            throw new IllegalArgumentException("Transaction invalide");
        }
        if (tx.getStatut() != StatutTransaction.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Transaction déjà traitée");
        }

        BigDecimal montant = tx.getMontant();

        // RÉCUPÉRATION DU WALLET VIA wallet_id (nouvelle architecture)
        Wallet wallet = walletRepository.findById(tx.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        wallet.validerRetrait(montant);
        walletRepository.save(wallet);

        tx.setStatut(StatutTransaction.SUCCESS);
        tx.setCompletedAt(LocalDateTime.now());
        tx.setDescription("Retrait validé par admin — fonds retirables");
        transactionRepository.save(tx);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Retrait validé ! Fonds disponibles pour retrait externe"));
    }

    // ==================================================================
    // 4. Admin : Rejeter un retrait (USER)
    // ==================================================================
    @PatchMapping("/{id}/rejeter-retrait")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<TransactionDTO> rejeterRetrait(
            @PathVariable Long id,
            @RequestBody RejetRetraitRequest request) {

        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction introuvable"));

        if (tx.getType() != TypeTransaction.RETRAIT || tx.getWalletType() != WalletType.USER) {
            throw new IllegalArgumentException("Transaction invalide");
        }
        if (tx.getStatut() != StatutTransaction.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Transaction déjà traitée");
        }

        Wallet wallet = walletRepository.findById(tx.getWalletId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        wallet.debloquerFonds(tx.getMontant());
        walletRepository.save(wallet);

        tx.setStatut(StatutTransaction.REJETEE);
        tx.setDescription("Rejeté : " + request.motif());
        transactionRepository.save(tx);

        return ResponseEntity.ok(dtoConverter.toTransactionDto(tx));
    }

    // ==================================================================
    // Helper
    // ==================================================================
  private Long extractUserId(UserDetails userDetails) {
    if (userDetails == null) {
        throw new IllegalStateException("Utilisateur non authentifié");
    }

    String login = userDetails.getUsername();

    return userRepository.findByLoginForAuth(login)  // CHARGE LES RÔLES → plus jamais LazyException
            .map(User::getId)
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + login));
}
}