package growzapp.backend.module.fournisseur.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.fournisseur.dto.ArticleFournisseurDTO;
import growzapp.backend.module.fournisseur.dto.FournisseurBrouillonDTO;
import growzapp.backend.module.fournisseur.dto.FournisseurDTO;
import growzapp.backend.module.fournisseur.dto.PartenaireDTO;
import growzapp.backend.module.fournisseur.model.Fournisseur;
import growzapp.backend.module.fournisseur.service.FournisseurService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs")
public class FournisseurController {

    private final FournisseurService fournisseurService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @PutMapping("/moi")
    @Operation(summary = "Enregistrer/mettre à jour mon brouillon de fiche fournisseur", description = "Aucun champ obligatoire — sauvegardable à tout moment avant soumission finale.")
    public ApiResponseDTO<FournisseurDTO> enregistrerBrouillon(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody FournisseurBrouillonDTO dto) {
        User user = getCurrentUser(userDetails);
        Fournisseur saved = fournisseurService.enregistrerBrouillon(user, dto);
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }

    @PostMapping("/moi/soumettre")
    @Operation(summary = "Soumettre définitivement ma fiche fournisseur à l'admin", description = "Valide que tous les champs obligatoires sont renseignés puis notifie l'équipe GrowzApp.")
    public ApiResponseDTO<FournisseurDTO> soumettre(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Fournisseur saved = fournisseurService.soumettre(user);
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }

    @GetMapping("/moi")
    @Operation(summary = "Consulter ma propre fiche fournisseur")
    public ApiResponseDTO<FournisseurDTO> maFiche(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Fournisseur f = fournisseurService.getByUserId(user.getId());
        return ApiResponseDTO.success(fournisseurService.toDto(f));
    }

    @PostMapping(value = "/moi/logo", consumes = "multipart/form-data")
    @Operation(summary = "Mettre à jour mon logo", description = "Affiché publiquement dans la section \"Nos partenaires\" du site une fois la fiche validée.")
    public ApiResponseDTO<FournisseurDTO> mettreAJourLogo(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestPart("logo") MultipartFile logo) {
        User user = getCurrentUser(userDetails);
        Fournisseur saved = fournisseurService.mettreAJourLogo(user.getId(), logo);
        return ApiResponseDTO.success(fournisseurService.toDto(saved));
    }

    @GetMapping("/partenaires")
    @Operation(summary = "Lister les fournisseurs partenaires (logo public)", description = "Endpoint public — fournisseurs validés ayant renseigné un logo, pour la section \"Nos partenaires\" du footer.")
    public ApiResponseDTO<List<PartenaireDTO>> partenaires() {
        return ApiResponseDTO.success(fournisseurService.getPartenaires());
    }

    @GetMapping
    @Operation(summary = "Rechercher des fournisseurs validés", description = "Filtrable par ville, pays et secteur — utilisé par les porteurs pour trouver un fournisseur.")
    public ApiResponseDTO<List<FournisseurDTO>> rechercher(
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) String pays,
            @RequestParam(required = false) Long secteurId) {
        List<FournisseurDTO> dtos = fournisseurService.rechercher(ville, pays, secteurId).stream()
                .map(fournisseurService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un fournisseur validé par ID")
    public ApiResponseDTO<FournisseurDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(fournisseurService.toDto(fournisseurService.getById(id)));
    }

    @GetMapping("/{id}/articles")
    @Operation(summary = "Lister les articles disponibles d'un fournisseur")
    public ApiResponseDTO<List<ArticleFournisseurDTO>> articles(@PathVariable Long id) {
        List<ArticleFournisseurDTO> dtos = fournisseurService.getArticles(id, true).stream()
                .map(fournisseurService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }
}
