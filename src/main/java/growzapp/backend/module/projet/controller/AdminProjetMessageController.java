package growzapp.backend.module.projet.controller;

import growzapp.backend.module.projet.dto.InvestisseurSimpleDTO;
import growzapp.backend.module.projet.dto.ProjetMessageAdminCreateDTO;
import growzapp.backend.module.projet.dto.ProjetMessageDTO;
import growzapp.backend.module.projet.service.ProjetMessageService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Canal de messagerie par projet — côté admin/communicant. Porteur
// strictement exclu (confirmé) : ADMIN et COMMUNICANT uniquement, même
// patron de rôle que NewsController pour la gestion des actualités.
@RestController
@RequestMapping("/api/admin/projets/{projetId}/messages")
@RequiredArgsConstructor
@Tag(name = "Admin - Messagerie projet", description = "Messagerie entre admin/communicant et investisseurs d'un projet")
public class AdminProjetMessageController {

    private final ProjetMessageService projetMessageService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNICANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Lister tous les messages d'un projet", tags = {"Admin - Messagerie projet"})
    public ApiResponseDTO<List<ProjetMessageDTO>> lister(@PathVariable Long projetId) {
        return ApiResponseDTO.success(projetMessageService.listerPourAdmin(projetId));
    }

    @GetMapping("/destinataires")
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNICANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Lister les investisseurs distincts du projet, pour le sélecteur de destinataires",
            tags = {"Admin - Messagerie projet"})
    public ApiResponseDTO<List<InvestisseurSimpleDTO>> destinataires(@PathVariable Long projetId) {
        return ApiResponseDTO.success(projetMessageService.listerDestinataires(projetId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COMMUNICANT')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Envoyer un message à tous les investisseurs du projet ou à un sous-ensemble ciblé",
            tags = {"Admin - Messagerie projet"})
    public ApiResponseDTO<ProjetMessageDTO> envoyer(
            @PathVariable Long projetId,
            @Valid @RequestBody ProjetMessageAdminCreateDTO dto,
            Authentication auth) {
        User auteur = userRepository.findByLoginForAuth(auth.getName())
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));
        ProjetMessageDTO message = projetMessageService.envoyerParAdmin(
                projetId, auteur, dto.contenu(), dto.destinataireType(), dto.destinataireIds());
        return ApiResponseDTO.success(message);
    }
}
