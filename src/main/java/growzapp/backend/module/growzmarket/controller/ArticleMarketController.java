package growzapp.backend.module.growzmarket.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.growzmarket.dto.ArticleMarketCreateDTO;
import growzapp.backend.module.growzmarket.dto.ArticleMarketDTO;
import growzapp.backend.module.growzmarket.model.ArticleMarket;
import growzapp.backend.module.growzmarket.service.ArticleMarketService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/market/articles")
@RequiredArgsConstructor
@Tag(name = "GrowzMarket - Articles")
public class ArticleMarketController {

    private final ArticleMarketService articleMarketService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @GetMapping
    @Operation(summary = "Catalogue public GrowzMarket", description = "Articles disponibles, tous porteurs confondus.")
    public ApiResponseDTO<List<ArticleMarketDTO>> catalogue() {
        List<ArticleMarketDTO> dtos = articleMarketService.getCatalogue().stream()
                .map(articleMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un article GrowzMarket")
    public ApiResponseDTO<ArticleMarketDTO> detail(@PathVariable Long id) {
        return ApiResponseDTO.success(articleMarketService.toDto(articleMarketService.getById(id)));
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Ajouter un article à Ma Boutique (porteur)", description = "'article' contient les données JSON (ArticleMarketCreateDTO), 'photos' les images optionnelles (plusieurs possibles).")
    public ApiResponseDTO<ArticleMarketDTO> ajouter(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestPart("article") ArticleMarketCreateDTO dto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        User user = getCurrentUser(userDetails);
        ArticleMarket saved = articleMarketService.creerArticle(user, dto, photos);
        return ApiResponseDTO.success(articleMarketService.toDto(saved));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @Operation(summary = "Modifier un article de Ma Boutique (porteur)")
    public ApiResponseDTO<ArticleMarketDTO> modifier(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestPart("article") ArticleMarketCreateDTO dto,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {
        User user = getCurrentUser(userDetails);
        ArticleMarket saved = articleMarketService.modifierArticle(id, user.getId(), dto, photos);
        return ApiResponseDTO.success(articleMarketService.toDto(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article de Ma Boutique (porteur)")
    public ApiResponseDTO<String> supprimer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        articleMarketService.supprimerArticle(id, user.getId());
        return ApiResponseDTO.<String>success(null).message("Article supprimé");
    }
}
