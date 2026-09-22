package growzapp.backend.module.fournisseur.controller;

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

import growzapp.backend.module.fournisseur.dto.ArticleFournisseurCreateDTO;
import growzapp.backend.module.fournisseur.dto.ArticleFournisseurDTO;
import growzapp.backend.module.fournisseur.model.ArticleFournisseur;
import growzapp.backend.module.fournisseur.model.Fournisseur;
import growzapp.backend.module.fournisseur.service.FournisseurService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fournisseurs/moi/articles")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs - Mon catalogue")
public class FournisseurEspaceController {

    private final FournisseurService fournisseurService;
    private final UserRepository userRepository;

    private Fournisseur getMonFournisseur(UserDetails userDetails) {
        User user = userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return fournisseurService.getByUserId(user.getId());
    }

    @GetMapping
    @Operation(summary = "Lister tous mes articles (disponibles ou non)")
    public ApiResponseDTO<List<ArticleFournisseurDTO>> mesArticles(@AuthenticationPrincipal UserDetails userDetails) {
        Fournisseur f = getMonFournisseur(userDetails);
        List<ArticleFournisseurDTO> dtos = fournisseurService.getArticles(f.getId(), false).stream()
                .map(fournisseurService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping(consumes = "multipart/form-data")
    @Operation(summary = "Ajouter un article/service à mon catalogue", description = "'article' contient les données JSON (ArticleFournisseurCreateDTO), 'photo' est l'image optionnelle.")
    public ApiResponseDTO<ArticleFournisseurDTO> ajouter(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestPart("article") ArticleFournisseurCreateDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Fournisseur f = getMonFournisseur(userDetails);
        ArticleFournisseur saved = fournisseurService.ajouterArticle(f.getId(), dto, photo);
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @Operation(summary = "Modifier un article de mon catalogue", description = "'article' contient les données JSON (ArticleFournisseurCreateDTO), 'photo' est l'image optionnelle (remplace la précédente si fournie).")
    public ApiResponseDTO<ArticleFournisseurDTO> modifier(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestPart("article") ArticleFournisseurCreateDTO dto,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        Fournisseur f = getMonFournisseur(userDetails);
        ArticleFournisseur saved = fournisseurService.modifierArticle(id, f.getId(), dto, photo);
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un article de mon catalogue")
    public ApiResponseDTO<String> supprimer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Fournisseur f = getMonFournisseur(userDetails);
        fournisseurService.supprimerArticle(id, f.getId());
        return ApiResponseDTO.<String>success(null).message("Article supprimé");
    }
}
