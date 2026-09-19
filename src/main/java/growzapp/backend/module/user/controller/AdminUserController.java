package growzapp.backend.module.user.controller;

import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.dto.UserDTO;
import growzapp.backend.module.user.repository.RoleRepository;
import growzapp.backend.module.user.service.UserService;
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
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/users", "/api/admin/users"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Utilisateurs", description = "Gestion complète des comptes utilisateurs par l'administrateur : consultation, création, modification des rôles, activation/désactivation et suppression")
public class AdminUserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    @GetMapping
    @Operation(
        summary = "Lister les utilisateurs (paginé)",
        description = "Retourne la liste paginée des utilisateurs avec filtre de recherche optionnel (nom, prénom, email ou login).",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page d'utilisateurs",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Page<UserDTO>> getAll(
            @Parameter(description = "Terme de recherche (nom, prénom, email ou login)", example = "john")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filtrer par rôle (ex: ADMIN, USER, COMMUNICANT)", example = "ADMIN")
            @RequestParam(required = false) String role,
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ApiResponseDTO.success(userService.getAllAdminPaged(search, role, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Détail d'un utilisateur",
        description = "Retourne le profil complet d'un utilisateur par son identifiant, incluant ses rôles, son statut KYC et ses investissements.",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur trouvé",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<UserDTO> getUserById(
            @Parameter(description = "Identifiant de l'utilisateur", example = "42", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(userService.getUserDtoById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Créer un utilisateur",
        description = "Crée un nouveau compte utilisateur avec les rôles forcés par l'administrateur. Le mot de passe doit être fourni dans le corps de la requête.",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Utilisateur créé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        UserDTO created = userService.createUserByAdmin(userDTO);
        return ApiResponseDTO.success(created).message("Utilisateur créé avec succès");
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Modifier un utilisateur",
        description = "Met à jour l'intégralité du profil d'un utilisateur. L'identifiant dans l'URL prend la priorité sur celui du corps.",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur mis à jour",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<UserDTO> updateUser(
            @Parameter(description = "Identifiant de l'utilisateur à modifier", example = "42", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {

        userDTO.setId(id);
        UserDTO updated = userService.updateUserByAdmin(userDTO);
        return ApiResponseDTO.success(updated).message("Utilisateur mis à jour avec succès");
    }

    @GetMapping("/roles")
    @Operation(
        summary = "Lister tous les rôles disponibles",
        description = "Retourne la liste des rôles existants sur la plateforme, triée alphabétiquement (ex: ADMIN, USER).",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des rôles",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "[\"ADMIN\", \"USER\"]")))
    })
    public List<String> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> role.getRole())
                .sorted()
                .toList();
    }

    @PatchMapping("/{id}/roles")
    @Operation(
        summary = "Mettre à jour les rôles d'un utilisateur",
        description = "Remplace intégralement les rôles d'un utilisateur par la liste fournie. Exemple : [\"ADMIN\", \"USER\"].",
        tags = {"Admin - Utilisateurs"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Liste des nouveaux rôles à assigner",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "[\"USER\"]")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rôles mis à jour avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<UserDTO> updateRoles(
            @Parameter(description = "Identifiant de l'utilisateur", example = "42", required = true)
            @PathVariable Long id,
            @RequestBody List<String> roles) {

        UserDTO updated = userService.updateUserRoles(id, roles);
        return ApiResponseDTO.success(updated).message("Rôles mis à jour avec succès");
    }

    @PatchMapping("/{id}/toggle")
    @Operation(
        summary = "Activer / Désactiver un compte",
        description = "Bascule l'état d'activation du compte utilisateur. Si le compte est actif, il sera désactivé, et inversement.",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statut du compte modifié",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<UserDTO> toggleUserStatus(
            @Parameter(description = "Identifiant de l'utilisateur", example = "42", required = true)
            @PathVariable Long id) {
        UserDTO updated = userService.toggleUserEnabled(id);
        String status = updated.isEnabled() ? "activé" : "désactivé";
        return ApiResponseDTO.success(updated).message("Compte " + status + " avec succès");
    }

    @PostMapping("/{id}/reset-password")
    @Operation(
        summary = "Réinitialiser le mot de passe d'un utilisateur (assisté par l'admin)",
        description = "Mécanisme de secours quand l'utilisateur n'a pas d'accès email fiable (courant en Afrique). "
            + "L'admin DOIT avoir vérifié l'identité hors application (téléphone/WhatsApp, comparaison avec les "
            + "documents KYC déjà en base — cf panel KYC) avant d'appeler cet endpoint : le motif de vérification "
            + "est obligatoire et conservé pour traçabilité. Génère un mot de passe temporaire à usage unique "
            + "(l'utilisateur devra le changer à sa prochaine connexion) et débloque le compte s'il était verrouillé. "
            + "Le mot de passe temporaire est retourné en clair UNE SEULE FOIS — à communiquer immédiatement par "
            + "téléphone, jamais par email ni sauvegardé ailleurs.",
        tags = {"Admin - Utilisateurs"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Motif de vérification d'identité (obligatoire)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"motifVerification\": \"Appel téléphonique — pièce d'identité comparée au recto KYC, correspond.\"}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mot de passe temporaire généré",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Motif de vérification manquant",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> resetPassword(
            @Parameter(description = "Identifiant de l'utilisateur", example = "42", required = true)
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body,
            org.springframework.security.core.Authentication authentication) {

        String motif = body.get("motifVerification");
        String tempPassword = userService.resetPasswordByAdmin(id, authentication.getName(), motif);

        return ApiResponseDTO.success(tempPassword)
                .message("Mot de passe temporaire généré — communiquez-le immédiatement à l'utilisateur par téléphone.");
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un utilisateur (corbeille)",
        description = "Suppression logique (soft delete) : le compte est masqué des listes actives et ne peut plus se connecter, mais reste restaurable depuis la corbeille. Aucune donnée n'est perdue.",
        tags = {"Admin - Utilisateurs"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utilisateur déplacé dans la corbeille",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<String> deleteUser(
            @Parameter(description = "Identifiant de l'utilisateur à supprimer", example = "42", required = true)
            @PathVariable Long id,
            @Parameter(description = "Motif de suppression (facultatif, conservé pour audit)")
            @RequestParam(required = false) String motif,
            org.springframework.security.core.Authentication authentication) {
        userService.softDeleteUser(id, authentication.getName(), motif);
        return ApiResponseDTO.<String>success(null).message("Utilisateur déplacé dans la corbeille");
    }

    @GetMapping("/corbeille")
    @Operation(summary = "Lister les utilisateurs supprimés (corbeille)", tags = {"Admin - Utilisateurs"})
    public ApiResponseDTO<Page<UserDTO>> getCorbeille(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return ApiResponseDTO.success(userService.getArchivedUsers(search, pageable));
    }

    @PostMapping("/{id}/restaurer")
    @Operation(summary = "Restaurer un utilisateur depuis la corbeille", tags = {"Admin - Utilisateurs"})
    public ApiResponseDTO<String> restaurerUser(@PathVariable Long id) {
        userService.restaurerUser(id);
        return ApiResponseDTO.<String>success(null).message("Utilisateur restauré");
    }

    @DeleteMapping("/{id}/purger")
    @Operation(
        summary = "Supprimer définitivement un utilisateur",
        description = "Purge définitive et irréversible d'un utilisateur déjà présent dans la corbeille. Toutes ses données associées sont perdues.",
        tags = {"Admin - Utilisateurs"}
    )
    public ApiResponseDTO<String> purgerUser(@PathVariable Long id) {
        userService.purgerUser(id);
        return ApiResponseDTO.<String>success(null).message("Utilisateur supprimé définitivement");
    }
}
