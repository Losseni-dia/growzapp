package growzapp.backend.module.investissement.controller;

import growzapp.backend.module.investissement.dto.InvestissementCreateDTO;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.mapper.InvestissementMapper;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.investissement.service.InvestissementService;
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
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/investissements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Investissements", description = "Création, consultation et gestion des investissements — accessible aux utilisateurs connectés et aux administrateurs selon l'endpoint")
public class InvestissementController {

    private final InvestissementRepository investissementRepository;
    private final InvestissementService investissementService;
    private final UserRepository userRepository;
    private final InvestissementMapper investissementMapper;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Créer un investissement", description = "Soumet un nouvel investissement sur un projet. L'identifiant de l'investisseur est extrait automatiquement du token JWT. Le statut initial est EN_ATTENTE jusqu'à validation par un administrateur.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Investissement créé en attente de validation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> create(
            @RequestBody InvestissementCreateDTO dto,
            Authentication auth) {
        User investisseur = (User) auth.getPrincipal();
        dto.setInvestisseurId(investisseur.getId());
        InvestissementDTO created = investissementService.save(dto, null);
        return ApiResponseDTO.success(created)
                .message("Investissement enregistré avec succès. En attente de validation admin.");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @investissementSecurityService.isOwnerAndPending(#id, authentication.principal.id)")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Modifier un investissement", description = "Modifie un investissement existant. Accessible à l'administrateur ou à l'investisseur propriétaire tant que l'investissement est en statut EN_ATTENTE.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Investissement modifié avec succès", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — non propriétaire ou statut non modifiable", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> update(
            @Parameter(description = "Identifiant de l'investissement à modifier", example = "15", required = true) @PathVariable Long id,
            @RequestBody InvestissementCreateDTO dto,
            Authentication auth) {
        User investisseur = (User) auth.getPrincipal();
        dto.setInvestisseurId(investisseur.getId());
        InvestissementDTO updated = investissementService.save(dto, id);
        return ApiResponseDTO.success(updated).message("Investissement modifié avec succès");
    }

    @GetMapping("/mes-investissements")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Mes investissements", description = "Retourne la liste de tous les investissements de l'utilisateur connecté, avec le détail des dividendes et du ROI.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste des investissements de l'utilisateur", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<InvestissementDTO>> getMyInvestments(Authentication authentication) {
        User user = userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        List<InvestissementDTO> mesInvestissements = investissementService.getByInvestisseurId(user.getId());
        return ApiResponseDTO.success(mesInvestissements);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Lister tous les investissements", description = "Retourne la liste complète de tous les investissements de la plateforme. Réservé aux administrateurs.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Liste complète des investissements", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<InvestissementDTO>> getAll() {
        return ApiResponseDTO.success(investissementService.getAllInvestissements());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @investissementSecurityService.isOwner(#id, authentication.principal.id)")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Détail d'un investissement", description = "Retourne le détail complet d'un investissement. Accessible à l'administrateur ou à l'investisseur propriétaire.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Détail de l'investissement", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Investissement introuvable", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> getById(
            @Parameter(description = "Identifiant de l'investissement", example = "15", required = true) @PathVariable Long id) {
        return ApiResponseDTO.success(investissementService.getInvestissementDtoById(id));
    }

    @PostMapping("/{id}/valider")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Valider un investissement", description = "Valide un investissement en statut EN_ATTENTE, génère le contrat et l'envoie par email à l'investisseur. Réservé aux administrateurs.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Investissement validé et contrat envoyé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Investissement introuvable", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> valider(
            @Parameter(description = "Identifiant de l'investissement à valider", example = "15", required = true) @PathVariable Long id)
            throws Exception {
        Investissement investissement = investissementService.validerInvestissement(id);
        InvestissementDTO dto = investissementMapper.toDto(investissement);
        return ApiResponseDTO.success(dto).message("Investissement validé et contrat envoyé par email");
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Refuser un investissement avec motif", description = "Refuse l'investissement, restitue les fonds dans le wallet de l'investisseur et lui envoie une notification avec le motif du refus. Réservé aux administrateurs.", tags = {
            "Investissements" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Motif du refus (optionnel)", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"motif\": \"Documents KYC insuffisants\"}"))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Investissement refusé — fonds restitués et investisseur notifié", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<InvestissementDTO> annuler(
            @Parameter(description = "Identifiant de l'investissement à refuser", example = "15", required = true) @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String motif = (body != null && body.containsKey("motif") && !body.get("motif").isBlank())
                ? body.get("motif")
                : "Refusé par l'administration";
        investissementService.annulerInvestissement(id, motif);
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);
        return ApiResponseDTO.success(dto).message("Investissement refusé — fonds restitués et investisseur notifié");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Supprimer un investissement", description = "Supprime définitivement un investissement de la base de données. Action irréversible. Réservé aux administrateurs.", tags = {
            "Investissements" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Investissement supprimé", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> delete(
            @Parameter(description = "Identifiant de l'investissement à supprimer", example = "15", required = true) @PathVariable Long id) {
        investissementRepository.deleteById(id);
        return ApiResponseDTO.success("Investissement supprimé");
    }
}