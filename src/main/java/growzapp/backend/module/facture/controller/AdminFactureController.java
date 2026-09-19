package growzapp.backend.module.facture.controller;

import growzapp.backend.module.facture.dto.FactureAdminDTO;
import growzapp.backend.module.facture.enums.StatutFacture;
import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.facture.repository.FactureRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
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
@RequestMapping({"/api/v1/admin/factures", "/api/admin/factures"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Factures", description = "Consultation en lecture seule de toutes les factures émises sur la plateforme")
public class AdminFactureController {

    private final FactureRepository factureRepository;

    @GetMapping
    @Operation(
        summary = "Lister les factures (paginé)",
        description = "Retourne la liste paginée de toutes les factures, avec recherche et filtre par statut. Réservé aux administrateurs.",
        tags = {"Admin - Factures"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page de factures retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Page<FactureAdminDTO>> getAll(
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Recherche par numéro de facture, nom ou email de l'investisseur")
            @RequestParam(required = false) String search,
            @Parameter(description = "Statut de la facture (EMISE, PAYEE, ANNULEE)")
            @RequestParam(required = false) String statut,
            @Parameter(description = "true pour afficher uniquement les factures archivées")
            @RequestParam(required = false, defaultValue = "false") boolean archive) {
        Pageable pageable = PageRequest.of(page, size);
        StatutFacture statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try {
                statutEnum = StatutFacture.valueOf(statut);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ApiResponseDTO.success(
                factureRepository.rechercherAdminDTO(search, statutEnum, archive, pageable));
    }

    @PostMapping("/{id}/archiver")
    @Operation(summary = "Archiver une facture", description = "Masque une facture des listes actives sans jamais la supprimer (document légal). Réversible via /desarchiver.", tags = {"Admin - Factures"})
    public ApiResponseDTO<String> archiver(@PathVariable Long id,
            org.springframework.security.core.Authentication authentication) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        facture.setArchiveLe(java.time.LocalDateTime.now());
        facture.setArchivePar(authentication.getName());
        factureRepository.save(facture);
        return ApiResponseDTO.<String>success(null).message("Facture archivée");
    }

    @PostMapping("/{id}/desarchiver")
    @Operation(summary = "Désarchiver une facture", tags = {"Admin - Factures"})
    public ApiResponseDTO<String> desarchiver(@PathVariable Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        facture.setArchiveLe(null);
        facture.setArchivePar(null);
        factureRepository.save(facture);
        return ApiResponseDTO.<String>success(null).message("Facture désarchivée");
    }
}
