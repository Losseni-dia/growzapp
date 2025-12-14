// src/main/java/growzapp/backend/controller/api/ProjetRestController.java
// VERSION FINALE 2025 – UNIQUEMENT LES ENDPOINTS PUBLICS & UTILISATEURS

package growzapp.backend.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementRequestDto;
import growzapp.backend.model.dto.projetDTO.ProjetCreateDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjetRestController {

    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final FileUploadService fileUploadService;
    private final StripeDepositService stripeDepositService;
    private final InvestissementService investissementService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // LISTE DES PROJETS VALIDÉS (PUBLIQUE)
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAllPublic() {
        return ApiResponseDTO.success(projetService.getAll());
    }

    // DÉTAIL D'UN PROJET
    @GetMapping("/{id}")
    public ApiResponseDTO<ProjetDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(projetService.getById(id));
    }

    // CRÉATION D'UN PROJET
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ApiResponseDTO<ProjetDTO> create(
            Authentication authentication,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = getCurrentUser(authentication);

        try {
            ProjetCreateDTO dto = objectMapper.readValue(projetJson, ProjetCreateDTO.class);
            ProjetDTO saved = projetService.createFromCreateDto(dto, currentUser);

            if (poster != null && !poster.isEmpty()) {
                if (poster.getSize() > 10 * 1024 * 1024) {
                    return ApiResponseDTO.error("Le poster ne doit pas dépasser 10 Mo");
                }
                String posterUrl = fileUploadService.uploadPoster(poster, saved.id());
                Projet entity = projetRepository.findById(saved.id()).orElseThrow();
                entity.setPoster(posterUrl);
                projetRepository.save(entity);
                saved = saved.withPoster(posterUrl);
            }

            return ApiResponseDTO.success(saved)
                    .message("Projet soumis avec succès ! En attente de validation.");

        } catch (Exception e) {
            log.error("Erreur création projet", e);
            return ApiResponseDTO.error("Erreur lors de la création : " + e.getMessage());
        }
    }

    // MES PROJETS (PORTEUR)
    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<ProjetDTO>> getMyProjects(Authentication auth) {
        User user = getCurrentUser(auth);
        return ApiResponseDTO.success(projetService.getByPorteurId(user.getId()));
    }

    // INVESTIR DANS UN PROJET (classique)
    @PostMapping("/{projetId}/investir")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<InvestissementDTO> investir(
            @PathVariable Long projetId,
            @RequestBody InvestissementRequestDto dto,
            Authentication auth) {

        User user = getCurrentUser(auth);

        InvestissementDTO result = investissementService.investir(projetId, dto.nombrePartsPris(), user);

        return ApiResponseDTO.success(result)
                .message("Investissement enregistré ! En attente de validation admin.");
    }

    // INVESTIR PAR CARTE (Stripe)
    @PostMapping("/{projetId}/investir-carte")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> investirParCarte(
            @PathVariable Long projetId,
            Authentication authentication,
            @RequestBody Map<String, Integer> body) {

        User user = getCurrentUser(authentication);
        Integer nombreParts = body.get("nombreParts");

        if (nombreParts == null || nombreParts < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nombre de parts invalide"));
        }

        Projet projet = projetRepository.findById(projetId)
                .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

        int partsDisponibles = projet.getPartsDisponible() - projet.getPartsPrises();
        if (nombreParts > partsDisponibles) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Plus assez de parts disponibles (reste : " + partsDisponibles + ")"));
        }

        try {
            String redirectUrl = stripeDepositService.createInvestissementSession(
                    user.getId(),
                    projetId,
                    nombreParts,
                    projet.getLibelle(),
                    projet.getPrixUnePart());

            log.info("Redirection Stripe → user {} → projet {} → {} parts", user.getId(), projetId, nombreParts);
            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (Exception e) {
            log.error("Erreur session Stripe", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Impossible de créer le paiement"));
        }
    }

    // MÉTHODE PRIVÉE RÉUTILISABLE → évite la duplication + charge les rôles en
    // sécurité
    private User getCurrentUser(Authentication authentication) {
        return userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}