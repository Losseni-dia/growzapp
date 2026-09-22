package growzapp.backend.module.fournisseur.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.fournisseur.dto.FournisseurDTO;
import growzapp.backend.module.fournisseur.dto.MotifDTO;
import growzapp.backend.module.fournisseur.model.Fournisseur;
import growzapp.backend.module.fournisseur.service.FournisseurService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Admin - Fournisseurs")
public class AdminFournisseurController {

    private final FournisseurService fournisseurService;

    @GetMapping("/en-attente")
    @Operation(summary = "Lister les fournisseurs en attente de validation")
    public ApiResponseDTO<List<FournisseurDTO>> getEnAttente() {
        List<FournisseurDTO> dtos = fournisseurService.getEnAttente().stream()
                .map(fournisseurService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/{id}/valider")
    @Operation(summary = "Valider une fiche fournisseur", description = "Attribue le rôle FOURNISSEUR et permet de publier un catalogue / recevoir des commandes.")
    public ApiResponseDTO<FournisseurDTO> valider(@PathVariable Long id) {
        Fournisseur saved = fournisseurService.valider(id);
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }

    @PostMapping("/{id}/rejeter")
    @Operation(summary = "Rejeter une fiche fournisseur")
    public ApiResponseDTO<FournisseurDTO> rejeter(@PathVariable Long id, @Valid @RequestBody MotifDTO dto) {
        Fournisseur saved = fournisseurService.rejeter(id, dto.motif());
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }
}
