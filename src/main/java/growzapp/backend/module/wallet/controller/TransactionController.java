package growzapp.backend.module.wallet.controller;

import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.dto.RejetRetraitRequest;
import growzapp.backend.module.wallet.dto.TransactionDTO;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.mapper.TransactionMapper;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Transactions", description = "Consultation de l'historique des transactions et gestion des retraits (validation/rejet par l'admin)")
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

    @GetMapping("/retraits-en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "[Admin] Lister les retraits en attente",
        description = "Retourne toutes les demandes de retrait utilisateur dont le statut est EN_ATTENTE_VALIDATION. Réservé aux administrateurs.",
        tags = {"Transactions"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des retraits en attente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TransactionDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<TransactionDTO>> getPendingWithdrawals() {
        List<Transaction> transactions = transactionRepository
                .findByTypeAndStatutAndWalletType(
                        TypeTransaction.RETRAIT,
                        StatutTransaction.EN_ATTENTE_VALIDATION);

        List<TransactionDTO> dtos = transactions.stream()
                .map(transactionMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @PatchMapping("/{id}/valider-retrait")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Transactional
    @Operation(
        summary = "[Admin] Valider une demande de retrait",
        description = "Approuve une demande de retrait utilisateur. Le montant est transféré vers le solde retirable, prêt pour un payout externe. Réservé aux administrateurs.",
        tags = {"Transactions"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrait validé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"success\": true, \"message\": \"Retrait validé ! Fonds disponibles pour retrait externe\"}"))),
        @ApiResponse(responseCode = "400", description = "Transaction invalide ou déjà traitée",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Demande de retrait introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> validerRetrait(
            @Parameter(description = "Identifiant de la transaction à valider", example = "88", required = true)
            @PathVariable Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de retrait introuvable"));

        if (tx.getType() != TypeTransaction.RETRAIT || tx.getWalletType() != WalletType.USER) {
            throw new IllegalArgumentException("Transaction invalide");
        }
        if (tx.getStatut() != StatutTransaction.EN_ATTENTE_VALIDATION) {
            throw new IllegalStateException("Transaction déjà traitée");
        }

        BigDecimal montant = tx.getMontant();

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

    @PatchMapping("/{id}/rejeter-retrait")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Transactional
    @Operation(
        summary = "[Admin] Rejeter une demande de retrait",
        description = "Rejette une demande de retrait et restitue les fonds bloqués au solde disponible de l'utilisateur. Réservé aux administrateurs.",
        tags = {"Transactions"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Retrait rejeté — fonds restitués au wallet",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = TransactionDTO.class))),
        @ApiResponse(responseCode = "400", description = "Transaction invalide ou déjà traitée",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Transaction introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<TransactionDTO> rejeterRetrait(
            @Parameter(description = "Identifiant de la transaction à rejeter", example = "88", required = true)
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

        return ResponseEntity.ok(transactionMapper.toDto(tx));
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
