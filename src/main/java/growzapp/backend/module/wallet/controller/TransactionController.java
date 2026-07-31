package growzapp.backend.module.wallet.controller;

import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.dto.TransactionDTO;
import growzapp.backend.module.wallet.mapper.TransactionMapper;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Le retrait utilisateur est entièrement automatique (voir
 * UserWalletController / WithdrawalService) — il n'existe plus de file de
 * validation admin pour les retraits (queue EN_ATTENTE_VALIDATION supprimée,
 * elle n'était de toute façon jamais alimentée par le flux de retrait réel).
 */
@RestController
@RequestMapping({"/api/v1/transactions", "/api/transactions"})
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Consultation de l'historique des transactions du wallet")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;

    @GetMapping("/mes-transactions")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Historique de mes transactions",
        description = "Retourne toutes les transactions du wallet de l'utilisateur connecté, triées par date décroissante.",
        tags = {"Transactions"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des transactions retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TransactionDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Wallet introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = extractUserId(userDetails);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));

        List<Transaction> transactions = transactionRepository
                .findByWalletTypeAndWalletIdOrderByCreatedAtDesc(wallet.getId());

        List<TransactionDTO> dtos = transactions.stream()
                .map(transactionMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    private Long extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + userDetails.getUsername()));
    }
}
