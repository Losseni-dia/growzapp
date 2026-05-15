package growzapp.backend.module.projet.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementRequestDto;
import growzapp.backend.model.entite.User;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.FileUploadService;
import growzapp.backend.service.InvestissementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Projets", description = "Gestion du cycle de vie des investissements (Consultation, Création, Géo-recherche)")
public class ProjetRestController {

    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final FileUploadService fileUploadService;
    private final InvestissementService investissementService;
    private final ProjetMapper projetMapper; 
    private final ObjectMapper objectMapper;

    /**
     * LISTE DES PROJETS VALIDÉS (PUBLIQUE)
     */
    @Operation(summary = "Lister les projets validés", description = "Accès public. Ne renvoie que les projets avec le statut VALIDÉ.")
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAllPublic() {
        // Le service renvoie des Entités, le contrôleur mappe en DTO
        List<Projet> entities = projetService.getAllValid();
        return ApiResponseDTO.success(projetMapper.toDtoList(entities));
    }

    /**
     * DÉTAIL D'UN PROJET
     */
    @Operation(summary = "Détail d'un projet par ID")
    @ApiResponse(responseCode = "200", description = "Projet trouvé")
    @ApiResponse(responseCode = "404", description = "Projet introuvable")
    @GetMapping("/{id}")
    public ApiResponseDTO<ProjetDTO> getById(@PathVariable Long id) {
        Projet entity = projetService.getById(id);
        return ApiResponseDTO.success(projetMapper.toDto(entity));
    }

    /**
     * CRÉATION D'UN PROJET
     */
    @Operation(
        summary = "Soumettre un nouveau projet",
        description = "Requête Multipart. Envoyer le JSON dans la partie 'projet' et le fichier image dans 'poster'.",
        security = @SecurityRequirement(name = "BearerAuth")
    )
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<ProjetDTO> create(
            Authentication authentication,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = getCurrentUser(authentication);

        try {
            // 1. Conversion JSON -> CreateDTO
            ProjetCreateDTO createDto = objectMapper.readValue(projetJson, ProjetCreateDTO.class);

            // 2. Mapping DTO -> Entity (Ici dans le controller)
            Projet projetInitial = projetMapper.toEntity(createDto);

            // 3. Appel au service avec l'entité et les données brutes
            Projet saved = projetService.create(projetInitial, createDto.secteurNom(), createDto.localiteNom(),
                    currentUser);

            // 4. Gestion optionnelle du poster (post-persistance pour avoir l'ID)
            if (poster != null && !poster.isEmpty()) {
                if (poster.getSize() > 10 * 1024 * 1024) {
                    return ApiResponseDTO.error("Le poster ne doit pas dépasser 10 Mo");
                }
                String posterUrl = fileUploadService.uploadPoster(poster, saved.getId());
                saved.setPoster(posterUrl);
                saved = projetService.update(saved); // Mise à jour de l'entité
            }

            // 5. Retour en DTO
            return ApiResponseDTO.success(projetMapper.toDto(saved))
                    .message("Projet soumis avec succès ! En attente de validation.");

        } catch (Exception e) {
            log.error("Erreur création projet", e);
            return ApiResponseDTO.error("Erreur lors de la création : " + e.getMessage());
        }
    }

    /**
     * MES PROJETS (PORTEUR)
     */
    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<ProjetDTO>> getMyProjects(Authentication auth) {
        User user = getCurrentUser(auth);
        List<Projet> entities = projetService.getByPorteurId(user.getId());
        return ApiResponseDTO.success(projetMapper.toDtoList(entities));
    }

    /**
     * RECHERCHE GÉOGRAPHIQUE
     */
    @Operation(summary = "Recherche géographique de projets", description = "Rayon par défaut : 100km.")
    @GetMapping("/proche-de-moi")
    public ApiResponseDTO<List<ProjetDTO>> getProjetsProches(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "100") double rayon) {

        List<Projet> proches = projetService.findProjetsProches(lat, lon, rayon);
        return ApiResponseDTO.success(projetMapper.toDtoList(proches));
    }

    /**
     * RÉCUPÉRATION PAR SLUG
     */
    @Operation(summary = "Récupérer un projet par son Slug (SEO)")
    @GetMapping("/slug/{slug}")
    public ApiResponseDTO<ProjetDTO> getBySlug(@PathVariable String slug) {
        Projet entity = projetService.getBySlug(slug);
        return ApiResponseDTO.success(projetMapper.toDto(entity));
    }

    /**
     * INVESTIR DANS UN PROJET (Classique)
     */
    @Operation(summary = "Effectuer un investissement", security = @SecurityRequirement(name = "BearerAuth"))
    @PostMapping("/{projetId}/investir")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<InvestissementDTO> investir(
            @PathVariable Long projetId,
            @RequestBody InvestissementRequestDto dto,
            Authentication auth) {

        User user = getCurrentUser(auth);
        // Ici, investissementService doit aussi être nettoyé pour renvoyer une Entité
        // si tu suis la règle partout
        return ApiResponseDTO.success(investissementService.investir(projetId, dto.nombrePartsPris(), user));
    }

    // --- MÉTHODES PRIVÉES ---

    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}