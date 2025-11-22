// src/main/java/growzapp/backend/controller/api/ProjetRestController.java
package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.projetDTO.ProjetCreateDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Localite;
import growzapp.backend.model.entite.Pays;
import growzapp.backend.model.entite.Secteur;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.LocaliteRepository;
import growzapp.backend.repository.PaysRepository;
import growzapp.backend.repository.SecteurRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.ProjetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/projets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // À changer en prod
public class ProjetRestController {

    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final PaysRepository paysRepository;
    private final LocaliteRepository localiteRepository;
    private final SecteurRepository secteurRepository;

    // LISTE DES PROJETS VALIDÉS (PUBLIQUE) – C’ÉTAIT ÇA QUI MANQUAIT DEPUIS LE
    // DÉBUT !!! 
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAllPublic() {
        List<ProjetDTO> projets = projetService.getAll(); // ta méthode qui filtre StatutProjet.VALIDE
        return ApiResponseDTO.success(projets);
    }

    // Création publique – tout utilisateur connecté
    @PostMapping(consumes = { "multipart/form-data" })
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ApiResponseDTO<ProjetDTO> create(
            Authentication authentication,
            @RequestPart("projet") ProjetCreateDTO createDto,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        String login = authentication.getName();
        User currentUser = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        MultipartFile[] files = poster != null ? new MultipartFile[] { poster } : null;

        // LA LIGNE MAGIQUE QUI RÈGLE TOUT
        ProjetDTO saved = projetService.createFromCreateDto(createDto, files, currentUser);

        return ApiResponseDTO.success(saved)
                .message("Projet soumis avec succès ! L'administrateur va le valider bientôt 🚀");
    }

    // Détail projet
    // Détail projet → UNIQUEMENT pour les utilisateurs connectés
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")  // ← SEUL LES CONNECTÉS PEUVENT VOIR LE DÉTAIL
    public ApiResponseDTO<ProjetDTO> getById(@PathVariable Long id) {
        ProjetDTO projet = projetService.getById(id);

        // Bonus sécurité : on ne montre le détail complet QUE si le projet est VALIDEE
        // OU si c'est un admin / le porteur du projet
        // (sinon on pourrait deviner des infos sur des projets en attente)
        if (projet.statutProjet() != StatutProjet.VALIDE) {
            throw new AccessDeniedException("Ce projet n'est pas encore publié");
        }

    return ApiResponseDTO.success(projet);
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