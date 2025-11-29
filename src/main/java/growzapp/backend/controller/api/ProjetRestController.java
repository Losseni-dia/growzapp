// src/main/java/growzapp/backend/controller/api/ProjetRestController.java
// VERSION FINALE 2025 – UNIQUEMENT LES ENDPOINTS PUBLICS & UTILISATEURS
// AUCUN ENDPOINT ADMIN ICI

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
import growzapp.backend.service.FileUploadService;
import growzapp.backend.service.InvestissementService;
import growzapp.backend.service.ProjetService;
import growzapp.backend.service.StripeDepositService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

        User currentUser = userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

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
            return ApiResponseDTO.error("Erreur lors de la création : " + e.getMessage());
        }
    }

    // MES PROJETS (PORTEUR)
    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<List<ProjetDTO>> getMyProjects(Authentication auth) {
        User user = userRepository.findByLogin(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));
        return ApiResponseDTO.success(projetService.getByPorteurId(user.getId()));
    }

    // INVESTIR DANS UN PROJET
    @PostMapping("/{projetId}/investir")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<InvestissementDTO> investir(
            @PathVariable Long projetId,
            @RequestBody InvestissementRequestDto dto,
            Authentication auth) {

        User user = userRepository.findByLogin(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        InvestissementDTO result = investissementService.investir(projetId, dto.nombrePartsPris(), user);

        return ApiResponseDTO.success(result)
                .message("Investissement enregistré ! En attente de validation admin.");
    }

     @PostMapping("/{projetId}/investir-carte")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> investirParCarte(
            @PathVariable Long projetId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, Integer> body) {

        Long userId = getCurrentUserId(userDetails);
        Integer nombreParts = body.get("nombreParts");

        if (nombreParts == null || nombreParts < 1) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Nombre de parts invalide"));
        }

        try {
            Projet projet = projetRepository.findById(projetId)
                    .orElseThrow(() -> new IllegalStateException("Projet introuvable"));

            // Vérification des parts disponibles
            int partsDisponibles = projet.getPartsDisponible() - projet.getPartsPrises();
            if (nombreParts > partsDisponibles) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Plus assez de parts disponibles (reste : " + partsDisponibles + ")"));
            }

            // Génération de la session Stripe avec TA méthode existante
            String redirectUrl = stripeDepositService.createInvestissementSession(
                    userId,
                    projetId,
                    nombreParts,
                    projet.getLibelle(),
                    projet.getPrixUnePart() // Tu passes bien le prix une part ici
            );

            log.info("Redirection investissement Stripe générée pour user {} → projet {} → {} parts",
                    userId, projetId, nombreParts);

            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));

        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur création session investissement Stripe", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Impossible de créer le paiement"));
        }
    }


// Utility method to get current user ID
    private Long getCurrentUserId(UserDetails userDetails) {
        User user = userRepository.findByLogin(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return user.getId();
    }

    // SUPPRIMÉ DÉLIBÉRÉMENT :
    // @GetMapping("/admin/{projetId}/wallet/solde")
    // → DÉPLACÉ DANS PwrojetWalletControler.java
}