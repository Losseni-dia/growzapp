// src/main/java/growzapp/backend/controller/api/TransactionController.java

package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.walletDTOs.RejetRetraitRequest;
import growzapp.backend.model.dto.walletDTOs.TransactionDTO;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    // 1. Historique personnel (WalletPage)
    // ==================================================================
    @GetMapping("/mes-transactions")
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);
        List<Transaction> transactions = transactionRepository
                .findByWallet_UserIdOrderByCreatedAtDesc(userId);

        List<TransactionDTO> dtos = transactions.stream()
                .map(dtoConverter::toTransactionDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ==================================================================
    // 2. Retraits en attente (Admin)
    // ==================================================================
    @GetMapping("/retraits-en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransactionDTO>> getPendingWithdrawals() {
        List<Transaction> transactions = transactionRepository
                .findByTypeAndStatutOrderByCreatedAtDesc(
                        TypeTransaction.RETRAIT,
                        StatutTransaction.EN_ATTENTE_VALIDATION);

        List<TransactionDTO> dtos = transactions.stream()
                .map(dtoConverter::toTransactionDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    // ==================================================================
    // 3. Admin : Valider un retrait
    @PatchMapping("/{id}/valider-retrait")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> validerRetrait(@PathVariable Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de retrait introuvable"));

        if (tx.getType() != TypeTransaction.RETRAIT) {
            throw new IllegalArgumentException("Seule une transaction de type RETRAIT peut être validée");
        }
        if (tx.getStatut() != StatutTransaction.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Cette demande n'est plus en attente de validation");
        }

        Wallet wallet = tx.getWallet();
        BigDecimal montant = tx.getMontant();

        // CORRECT : bloqué → retirable (interne à l'app)
        wallet.validerRetrait(montant); // ta méthode qui fait bloqué → retirable
        walletRepository.save(wallet);

        // Mise à jour de la transaction
        tx.setStatut(StatutTransaction.SUCCESS);
        tx.setCompletedAt(LocalDateTime.now());
        tx.setDescription("Retrait validé par admin — fonds disponibles pour retrait externe");
        transactionRepository.save(tx);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Retrait validé ! Les fonds sont maintenant retirables."));
    }

    // ==================================================================
    // 4. Admin : Rejeter un retrait
    // ==================================================================
    @PatchMapping("/{id}/rejeter-retrait")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransactionDTO> rejeterRetrait(
            @PathVariable Long id,
            @RequestBody RejetRetraitRequest request) {

        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction introuvable"));

        if (tx.getType() != TypeTransaction.RETRAIT) {
            throw new IllegalArgumentException("Seule une transaction de type RETRAIT peut être rejetée");
        }
        if (tx.getStatut() != StatutTransaction.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Cette transaction n'est plus en attente");
        }

        // CORRIGÉ : plus besoin de conversion → montant est déjà BigDecimal
        tx.getWallet().debloquerFonds(tx.getMontant());

        tx.setStatut(StatutTransaction.REJETEE);
        tx.setDescription("Rejeté : " + request.motif());

        transactionRepository.save(tx);

        return ResponseEntity.ok(dtoConverter.toTransactionDto(tx));
    }

    // Helper propre, sûr, rapide
    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        String login = userDetails.getUsername();

        return userRepository.findByLogin(login)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable avec le login: " + login));
    }
}