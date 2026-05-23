package growzapp.backend.module.dividende.controller;

import growzapp.backend.module.dividende.dto.DividendeDTO;
import growzapp.backend.module.dividende.service.DividendeService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dividendes")
@RequiredArgsConstructor
@Tag(name = "Dividendes", description = "Consultation et gestion des dividendes liés aux investissements")
public class DividendeRestController {

    private final DividendeService dividendeService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(
        summary = "Lister tous les dividendes",
        description = "Retourne la liste complète de tous les dividendes de la plateforme.",
        tags = {"Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des dividendes",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<DividendeDTO>> getAll() {
        return ApiResponseDTO.success(dividendeService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Détail d'un dividende",
        description = "Retourne le détail complet d'un dividende par son identifiant.",
        tags = {"Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende trouvé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Dividende introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DividendeDTO> getById(
            @Parameter(description = "Identifiant du dividende", example = "33", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(dividendeService.getById(id));
    }

    @PostMapping
    @Operation(
        summary = "Créer un dividende",
        description = "Crée un nouveau dividende manuellement.",
        tags = {"Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende créé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DividendeDTO> create(@RequestBody DividendeDTO dto) {
        return ApiResponseDTO.success(dividendeService.save(dto));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Modifier un dividende",
        description = "Met à jour les informations d'un dividende existant.",
        tags = {"Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende mis à jour",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Dividende introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<DividendeDTO> update(
            @Parameter(description = "Identifiant du dividende à modifier", example = "33", required = true)
            @PathVariable Long id,
            @RequestBody DividendeDTO dto) {
        DividendeDTO updated = new DividendeDTO(
                id,
                dto.montantParPart(),
                dto.statutDividende(),
                dto.moyenPaiement(),
                dto.datePaiement(),
                dto.investissementId(),
                dto.investissementInfo(),
                dto.montantTotal(),
                dto.fileName(),
                dto.factureUrl(),
                dto.facture(),
                dto.motif()
        );
        return ApiResponseDTO.success(dividendeService.save(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un dividende",
        description = "Supprime définitivement un dividende. Action irréversible.",
        tags = {"Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dividende supprimé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant du dividende à supprimer", example = "33", required = true)
            @PathVariable Long id) {
        dividendeService.deleteById(id);
        return ApiResponseDTO.success(null);
    }

    @GetMapping("/mes-dividendes")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Mes dividendes",
        description = "Retourne la liste de tous les dividendes perçus par l'utilisateur connecté, toutes participations confondues.",
        tags = {"Dividendes"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des dividendes de l'utilisateur connecté",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<DividendeDTO>> getMesDividendes(Authentication authentication) {
        String login = authentication.getName();

        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return ApiResponseDTO.success(dividendeService.getByInvestisseurId(user.getId()));
    }
}
