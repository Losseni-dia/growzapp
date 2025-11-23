// src/main/java/growzapp/backend/controller/api/ProjetRestController.java
package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.projetDTO.ProjetCreateDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.FileUploadService;
import growzapp.backend.service.ProjetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // À changer en prod
public class ProjetRestController {

    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final FileUploadService fileUploadService;
   

    // LISTE DES PROJETS VALIDÉS (PUBLIQUE) – C’ÉTAIT ÇA QUI MANQUAIT DEPUIS LE
    // DÉBUT !!! 
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAllPublic() {
        List<ProjetDTO> projets = projetService.getAll(); // ta méthode qui filtre StatutProjet.VALIDE
        return ApiResponseDTO.success(projets);
    }

    // Création publique – tout utilisateur connecté
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("isAuthenticated")
    @Transactional
    public ApiResponseDTO<ProjetDTO> create(
            Authentication authentication,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        User currentUser = userRepository.findByLogin(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        try {
            ObjectMapper mapper = new ObjectMapper();
            ProjetCreateDTO dto = mapper.readValue(projetJson, ProjetCreateDTO.class);

            // 1. Création du projet (sans poster)
            ProjetDTO saved = projetService.createFromCreateDto(dto, currentUser);

            // 2. Upload du poster si présent
            if (poster != null && !poster.isEmpty()) {
                if (poster.getSize() > 10 * 1024 * 1024) {
                    return ApiResponseDTO.error("Poster trop volumineux (max 10 Mo)");
                }

                String posterUrl = fileUploadService.uploadPoster(poster, saved.id());

                // Mise à jour en base
                Projet entity = projetRepository.findById(saved.id()).orElseThrow();
                entity.setPoster(posterUrl);
                projetRepository.save(entity);

                // On renvoie un NOUVEAU DTO avec le poster (record = immuable)
                saved = saved.withPoster(posterUrl); // ← LA SEULE LIGNE À CHANGER
            }

            return ApiResponseDTO.success(saved)
                    .message("Projet soumis avec succès !");

        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponseDTO.error("Erreur : " + e.getMessage());
        }
    }
    
    // Mes projets (porteur)
    @GetMapping("/mes-projets")
    @PreAuthorize("isAuthenticated()") // ← TOUT USER CONNECTÉ
    public ApiResponseDTO<List<ProjetDTO>> getMyProjects(Authentication auth) {
        User user = (User) auth.getPrincipal();
        List<ProjetDTO> projects = projetService.getByPorteurId(user.getId());
        return ApiResponseDTO.success(projects);
    }

    // Dans ton ProjetController.java

   

   
}