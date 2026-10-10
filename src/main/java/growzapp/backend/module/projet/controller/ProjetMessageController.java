package growzapp.backend.module.projet.controller;

import growzapp.backend.module.projet.dto.ProjetMessageCreateDTO;
import growzapp.backend.module.projet.dto.ProjetMessageDTO;
import growzapp.backend.module.projet.service.ProjetMessageService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Canal de messagerie par projet — côté investisseur. Un investisseur ne
// voit que les messages dont il est l'auteur, ou les messages admin qui lui
// sont destinés (diffusion à tous ou ciblée sur lui). Porteur exclu.
@RestController
@RequestMapping("/api/projets/{projetId}/messages")
@RequiredArgsConstructor
@Tag(name = "Messagerie projet", description = "Messagerie entre un investisseur et l'équipe admin/communicant d'un projet")
public class ProjetMessageController {

    private final ProjetMessageService projetMessageService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Lister les messages visibles pour l'investisseur connecté sur ce projet",
            tags = {"Messagerie projet"})
    public ApiResponseDTO<List<ProjetMessageDTO>> lister(@PathVariable Long projetId, Authentication auth) {
        User investisseur = (User) auth.getPrincipal();
        return ApiResponseDTO.success(projetMessageService.listerPourInvestisseur(projetId, investisseur.getId()));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Répondre dans le canal de messagerie du projet", tags = {"Messagerie projet"})
    public ApiResponseDTO<ProjetMessageDTO> envoyer(
            @PathVariable Long projetId,
            @Valid @RequestBody ProjetMessageCreateDTO dto,
            Authentication auth) {
        User investisseur = (User) auth.getPrincipal();
        ProjetMessageDTO message = projetMessageService.envoyerParInvestisseur(projetId, investisseur, dto.contenu());
        return ApiResponseDTO.success(message);
    }
}
