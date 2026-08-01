package growzapp.backend.module.projet.controller;

import growzapp.backend.module.paiement.innerwallet.ProjetWithdrawalService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.service.UserService;
import growzapp.backend.module.wallet.dto.RetraitProjetPorteurRequest;
import growzapp.backend.module.wallet.dto.TransfertProjetPersonnelRequest;
import growzapp.backend.module.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Actions self-service du porteur sur le wallet de SON projet : retirer vers
 * Mobile Money, ou transférer vers son propre wallet personnel. Le déblocage
 * de trésorerie (soldeBloque → soldeDisponible) reste une action admin, gérée
 * dans ProjetWalletController — c'est le déblocage qui fait office de
 * validation ; aucun contrôle admin supplémentaire n'est requis ici.
 */
@Slf4j
@RestController
@RequestMapping("/api/projets/{projetId}/wallet")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Porteur - Trésorerie Projet", description = "Retrait et transfert self-service par le porteur depuis le soldeDisponible du wallet de son projet")
public class ProjetWalletSelfServiceController {

    private final ProjetRepository projetRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final ProjetWithdrawalService projetWithdrawalService;

    private boolean canAccess(Projet projet, User currentUser) {
        if (currentUser == null) {
            return false;
        }
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getRole()));
        boolean isOwner = projet.getPorteur() != null
                && projet.getPorteur().getId().equals(currentUser.getId());
        return isAdmin || isOwner;
    }

    @PostMapping("/retirer")
    @Operation(
        summary = "Retrait Mobile Money depuis le wallet du projet",
        description = "Le porteur retire un montant depuis le soldeDisponible du wallet de son projet vers un numéro Mobile Money. Réservé au porteur du projet (ou à un admin). Le virement bancaire n'est pas proposé ici (voir StripePayoutService).",
        tags = {"Porteur - Trésorerie Projet"}
    )
    public ResponseEntity<ApiResponseDTO<String>> retirer(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @Valid @RequestBody RetraitProjetPorteurRequest request) {

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        if (!canAccess(projet, userService.getCurrentUser())) {
            return ResponseEntity.status(403).body(ApiResponseDTO.error("Accès refusé"));
        }

        try {
            String txId = projetWithdrawalService.executerRetraitMobileMoney(
                    projetId, request.montant(), request.phone(), request.idempotencyKey());
            return ResponseEntity.ok(ApiResponseDTO.success("Retrait initié").message(txId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    @PostMapping("/transferer")
    @Operation(
        summary = "Transfert interne vers le wallet personnel du porteur",
        description = "Le porteur transfère un montant depuis le soldeDisponible du wallet de son projet vers son propre wallet personnel, instantanément (aucun prestataire externe). Réservé au porteur du projet (ou à un admin).",
        tags = {"Porteur - Trésorerie Projet"}
    )
    public ResponseEntity<ApiResponseDTO<String>> transferer(
            @Parameter(description = "Identifiant du projet", example = "7", required = true)
            @PathVariable Long projetId,
            @Valid @RequestBody TransfertProjetPersonnelRequest request) {

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        User currentUser = userService.getCurrentUser();
        if (!canAccess(projet, currentUser)) {
            return ResponseEntity.status(403).body(ApiResponseDTO.error("Accès refusé"));
        }
        if (projet.getPorteur() == null) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error("Ce projet n'a pas de porteur"));
        }

        try {
            walletService.transfererProjetVersPersonnel(
                    projetId, projet.getPorteur().getId(), request.montant(), request.idempotencyKey());
            return ResponseEntity.ok(ApiResponseDTO.success("Transfert effectué"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }
}
