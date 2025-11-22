package growzapp.backend.controller.api.admin;


import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.userDTO.UserDTO;
import growzapp.backend.service.UserService;
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
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Tout le controller est réservé aux admins
public class AdminUserController {

    private final UserService userService;

    // ==================================================================
    // 1. Liste paginée + recherche (par nom, prénom, email, login)
    // ==================================================================
    @GetMapping
    public ApiResponseDTO<List<UserDTO>> getAll(
            @RequestParam(required = false) String search) {

        List<UserDTO> users = userService.getAllAdmin(search);
        return ApiResponseDTO.success(users);
    }

    // ==================================================================
    // 2. Détail d’un utilisateur
    // ==================================================================
    @GetMapping("/{id}")
    public ApiResponseDTO<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserDtoById(id);
        return ApiResponseDTO.success(userDTO);
    }

    // ==================================================================
    // 3. Création d’un utilisateur par l’admin
    // ==================================================================
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponseDTO<UserDTO> createUser(@Valid @RequestBody UserDTO userDTO) {
        // L’admin peut forcer les rôles + le mot de passe est obligatoire à la création
        UserDTO created = userService.createUserByAdmin(userDTO);
        return ApiResponseDTO.success(created)
                .message("Utilisateur créé avec succès");
    }

    // ==================================================================
    // 4. Mise à jour complète
    // ==================================================================
    @PutMapping("/{id}")
    public ApiResponseDTO<UserDTO> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO userDTO) {

        userDTO.setId(id);
        UserDTO updated = userService.updateUserByAdmin(userDTO);
        return ApiResponseDTO.success(updated)
                .message("Utilisateur mis à jour avec succès");
    }

    // ==================================================================
    // 5. Modifier uniquement les rôles (pratique dans l’UI)
    // ==================================================================
    @PatchMapping("/{id}/roles")
    public ApiResponseDTO<UserDTO> updateRoles(
            @PathVariable Long id,
            @RequestBody List<String> roles) { // ex: ["ADMIN", "USER"]

        UserDTO updated = userService.updateUserRoles(id, roles);
        return ApiResponseDTO.success(updated)
                .message("Rôles mis à jour avec succès");
    }

    // ==================================================================
    // 6. Activer / Désactiver un compte (soft disable)
    // Tu peux ajouter un champ boolean enabled dans User si tu veux
    // ==================================================================
    @PatchMapping("/{id}/toggle")
    public ApiResponseDTO<UserDTO> toggleUserStatus(@PathVariable Long id) {
        UserDTO updated = userService.toggleUserEnabled(id);
        String status = updated.isEnabled() ? "activé" : "désactivé";
        return ApiResponseDTO.success(updated)
                .message("Compte " + status + " avec succès");
    }

    // ==================================================================
    // 7. Suppression (soft delete ou hard – ici hard, mais tu peux changer)
    // ==================================================================
    @DeleteMapping("/{id}")
    public ApiResponseDTO<String> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ApiResponseDTO.success("Utilisateur supprimé définitivement")
                .message("Suppression effectuée");
    }
}