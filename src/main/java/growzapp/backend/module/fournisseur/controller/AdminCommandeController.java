package growzapp.backend.module.fournisseur.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.fournisseur.dto.CommandeDTO;
import growzapp.backend.module.fournisseur.dto.MotifDTO;
import growzapp.backend.module.fournisseur.model.Commande;
import growzapp.backend.module.fournisseur.service.CommandeService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/commandes")
@RequiredArgsConstructor
@Tag(name = "Admin - Commandes fournisseur")
public class AdminCommandeController {

    private final CommandeService commandeService;

    @GetMapping("/en-attente")
    @Operation(summary = "Lister les commandes en attente de validation")
    public ApiResponseDTO<List<CommandeDTO>> getEnAttente() {
        List<CommandeDTO> dtos = commandeService.getEnAttenteAdmin().stream()
                .map(commandeService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/litiges")
    @Operation(summary = "Lister les commandes en litige")
    public ApiResponseDTO<List<CommandeDTO>> getLitiges() {
        List<CommandeDTO> dtos = commandeService.getEnLitigeAdmin().stream()
                .map(commandeService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/{id}/valider")
    @Operation(summary = "Valider une commande", description = "Débite le wallet du projet et séquestre les fonds au bénéfice du fournisseur.")
    public ApiResponseDTO<CommandeDTO> valider(@PathVariable Long id) {
        Commande saved = commandeService.validerAdmin(id);
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }

    @PostMapping("/{id}/rejeter")
    @Operation(summary = "Rejeter une commande avant validation")
    public ApiResponseDTO<CommandeDTO> rejeter(@PathVariable Long id, @Valid @RequestBody MotifDTO dto) {
        Commande saved = commandeService.rejeterAdmin(id, dto.motif());
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }

    @PostMapping("/{id}/arbitrer")
    @Operation(summary = "Arbitrer un litige", description = "enFaveurDuFournisseur=true libère les fonds au fournisseur, false les rembourse au wallet du projet.")
    public ApiResponseDTO<CommandeDTO> arbitrer(
            @PathVariable Long id,
            @RequestParam boolean enFaveurDuFournisseur,
            @Valid @RequestBody MotifDTO dto) {
        Commande saved = commandeService.arbitrerLitige(id, enFaveurDuFournisseur, dto.motif());
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }
}
