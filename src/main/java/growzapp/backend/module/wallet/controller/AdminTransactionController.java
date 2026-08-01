package growzapp.backend.module.wallet.controller;

import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.wallet.dto.TransactionAdminDTO;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.service.AdminTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/admin/transactions", "/api/admin/transactions"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Transactions", description = "Historique global des transactions de la plateforme : dépôts, retraits, investissements, versements porteur, dividendes")
public class AdminTransactionController {

    private final AdminTransactionService adminTransactionService;

    @GetMapping
    @Operation(
        summary = "Lister toutes les transactions (paginé)",
        description = "Retourne l'historique complet des transactions, tous types confondus (dépôts, retraits, investissements, versements porteur, dividendes...), avec les détails du wallet/utilisateur/projet lié à chacune. Réservé aux administrateurs.",
        tags = {"Admin - Transactions"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page de transactions retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Page<TransactionAdminDTO>> getAll(
            @Parameter(description = "Filtre par type de transaction (DEPOT, RETRAIT, INVESTISSEMENT, VERSEMENT_PORTEUR, VERSEMENT_DIVIDENDE...)")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filtre par type de wallet source (USER, PROJET, DIVIDENDE)")
            @RequestParam(required = false) String walletType,
            @Parameter(description = "Filtre par statut (SUCCESS, FAILED, EN_COURS...)")
            @RequestParam(required = false) String statut,
            @Parameter(description = "Recherche libre (description, référence externe)")
            @RequestParam(required = false) String search,
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        TypeTransaction typeEnum = parseEnum(TypeTransaction.class, type);
        WalletType walletTypeEnum = parseEnum(WalletType.class, walletType);
        StatutTransaction statutEnum = parseEnum(StatutTransaction.class, statut);

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDTO.success(
                adminTransactionService.getAllAdmin(typeEnum, walletTypeEnum, statutEnum, search, pageable));
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumClass, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
