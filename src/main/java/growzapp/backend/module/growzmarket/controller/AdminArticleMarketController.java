package growzapp.backend.module.growzmarket.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.growzmarket.dto.ArticleMarketDTO;
import growzapp.backend.module.growzmarket.model.ArticleMarket;
import growzapp.backend.module.growzmarket.service.ArticleMarketService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/market/articles")
@RequiredArgsConstructor
@Tag(name = "Admin - GrowzMarket")
public class AdminArticleMarketController {

    private final ArticleMarketService articleMarketService;

    @GetMapping
    @Operation(summary = "Lister tous les articles GrowzMarket, tous porteurs et statuts confondus")
    public ApiResponseDTO<List<ArticleMarketDTO>> getAll() {
        List<ArticleMarketDTO> dtos = articleMarketService.getAllForAdmin().stream()
                .map(articleMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/{id}/disponibilite")
    @Operation(summary = "Afficher ou masquer un article du catalogue public", description = "Modération admin — n'affecte pas la boutique du porteur, seulement sa visibilité dans le catalogue public.")
    public ApiResponseDTO<ArticleMarketDTO> setDisponibilite(
            @PathVariable Long id,
            @RequestParam boolean disponible) {
        ArticleMarket saved = articleMarketService.setDisponibiliteAdmin(id, disponible);
        return ApiResponseDTO.success(articleMarketService.toDto(saved))
                .message(disponible ? "Article réaffiché dans le catalogue" : "Article masqué du catalogue");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer définitivement un article (modération, abus)")
    public ApiResponseDTO<String> supprimer(@PathVariable Long id) {
        articleMarketService.supprimerArticleAdmin(id);
        return ApiResponseDTO.<String>success(null).message("Article supprimé");
    }
}
