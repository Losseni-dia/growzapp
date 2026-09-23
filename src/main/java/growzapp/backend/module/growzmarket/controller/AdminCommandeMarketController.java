package growzapp.backend.module.growzmarket.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.fournisseur.dto.MotifDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketDTO;
import growzapp.backend.module.growzmarket.model.CommandeMarket;
import growzapp.backend.module.growzmarket.service.CommandeMarketService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/market/commandes")
@RequiredArgsConstructor
@Tag(name = "Admin - GrowzMarket")
public class AdminCommandeMarketController {

    private final CommandeMarketService commandeMarketService;

    @GetMapping("/toutes")
    @Operation(summary = "Lister toutes les commandes GrowzMarket, tous statuts confondus")
    public ApiResponseDTO<List<CommandeMarketDTO>> getToutes() {
        List<CommandeMarketDTO> dtos = commandeMarketService.getToutesAdmin().stream()
                .map(commandeMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/litiges")
    @Operation(summary = "Lister les commandes GrowzMarket en litige")
    public ApiResponseDTO<List<CommandeMarketDTO>> getLitiges() {
        List<CommandeMarketDTO> dtos = commandeMarketService.getEnLitigeAdmin().stream()
                .map(commandeMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/{id}/arbitrer")
    @Operation(summary = "Arbitrer un litige GrowzMarket", description = "enFaveurDuVendeur=true confirme le retrait, false annule et rembourse l'acheteur.")
    public ApiResponseDTO<CommandeMarketDTO> arbitrer(
            @PathVariable Long id,
            @RequestParam boolean enFaveurDuVendeur,
            @Valid @RequestBody MotifDTO dto) {
        CommandeMarket saved = commandeMarketService.arbitrer(id, enFaveurDuVendeur, dto.motif());
        return ApiResponseDTO.success(commandeMarketService.toDto(saved));
    }
}
