package growzapp.backend.module.growzmarket.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.growzmarket.dto.ArticleMarketDTO;
import growzapp.backend.module.growzmarket.service.ArticleMarketService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// Endpoint séparé (hors du préfixe /api/market/articles, qui est
// entièrement public en lecture) pour ne jamais risquer d'exposer la liste
// complète des articles d'un porteur — y compris ses articles
// indisponibles — via le whitelisting public de SecurityConfig.
@RestController
@RequiredArgsConstructor
@Tag(name = "GrowzMarket - Ma boutique")
public class MesArticlesMarketController {

    private final ArticleMarketService articleMarketService;
    private final UserRepository userRepository;

    @GetMapping("/api/market/mes-articles")
    @Operation(summary = "Lister mes articles GrowzMarket (porteur)", description = "Inclut les articles indisponibles — vue de gestion, pas le catalogue public.")
    public ApiResponseDTO<List<ArticleMarketDTO>> mesArticles(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        List<ArticleMarketDTO> dtos = articleMarketService.getMesArticles(user.getId()).stream()
                .map(articleMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }
}
