package growzapp.backend.module.dividende.controller;

import growzapp.backend.module.dividende.dto.DividendeDTO;
import growzapp.backend.module.dividende.service.DividendeService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dividendes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Dividendes", description = "Gestion des dividendes par l'administrateur : création, modification, suppression et distribution globale aux investisseurs")
public class AdminDividendeController {

    private final DividendeService dividendeService;

    @GetMapping
    @Operation(
        summary = "Lister les dividendes (paginé)",
        description = "Retourne la liste paginée de tous les dividendes. Réservé aux administrateurs.",
        tags = {"Admin - Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page de dividendes retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Page<DividendeDTO>> getAll(
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "10")
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDTO.success(dividendeService.getAllAdmin(pageable));
    }

    @PostMapping
    @Operation(
        summary = "Créer un dividende",
        description = "Crée un nouveau dividende et le rattache à un investissement. Réservé aux administrateurs.",
        tags = {"Admin - Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende créé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Corps de la requête invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DividendeDTO> create(@Valid @RequestBody DividendeDTO dto) {
        return ApiResponseDTO.success(dividendeService.save(dto)).message("Dividende créé avec succès");
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Modifier un dividende",
        description = "Met à jour les informations d'un dividende existant. Réservé aux administrateurs.",
        tags = {"Admin - Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende mis à jour",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Dividende introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DividendeDTO> update(
            @Parameter(description = "Identifiant du dividende à modifier", example = "33", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DividendeDTO dto) {
        dto = new DividendeDTO(id, dto.montantParPart(), dto.statutDividende(), dto.moyenPaiement(),
                dto.datePaiement(), dto.investissementId(), dto.investissementInfo(),
                dto.montantTotal(), dto.fileName(), dto.factureUrl(), dto.facture(), dto.motif());
        return ApiResponseDTO.success(dividendeService.save(dto)).message("Dividende mis à jour");
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un dividende",
        description = "Supprime définitivement un dividende. Action irréversible. Réservé aux administrateurs.",
        tags = {"Admin - Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende supprimé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> delete(
            @Parameter(description = "Identifiant du dividende à supprimer", example = "33", required = true)
            @PathVariable Long id) {
        dividendeService.deleteById(id);
        return ApiResponseDTO.success("Dividende supprimé");
    }
}
