package growzapp.backend.controller.api.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.service.FileUploadService;
import growzapp.backend.service.ProjetService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/projets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjetController {

    private final ProjetService projetService;
    private final FileUploadService fileUploadService;
    private final ProjetRepository projetRepository;

    // Liste complète (avec recherche)
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAll(@RequestParam(required = false) String search) {
        List<ProjetDTO> projets = projetService.getAllAdmin(search);
        return ApiResponseDTO.success(projets)
                .message(projets.isEmpty() ? "Aucun projet trouvé" : "Projets récupérés avec succès");
    }

    // Détail d'un projet
    @GetMapping("/{id}")
    public ApiResponseDTO<ProjetDTO> getOne(@PathVariable Long id) {
        return ApiResponseDTO.success(projetService.getById(id));
    }

    // Modification complète (multipart : JSON + poster optionnel)
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponseDTO<ProjetDTO> update(
            @PathVariable Long id,
            @RequestPart("projet") String projetJson,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(projetJson);

            ProjetDTO updated = projetService.updateProjetFromJson(id, node);

            if (poster != null && !poster.isEmpty()) {
                String url = fileUploadService.uploadPoster(poster, id);
                Projet entity = projetRepository.findById(id).orElseThrow();
                entity.setPoster(url);
                projetRepository.save(entity);
                updated = updated.withPoster(url);
            }

            return ApiResponseDTO.success(updated);
        } catch (Exception e) {
            return ApiResponseDTO.error("Erreur : " + e.getMessage());
        }
    }

    // Changer le statut (valider / rejeter / remettre en préparation etc.)
    @PatchMapping("/{id}/statut")
    public ApiResponseDTO<ProjetDTO> changerStatut(
            @PathVariable Long id,
            @RequestBody StatutProjet nouveauStatut) {

        ProjetDTO updated = projetService.changerStatut(id, nouveauStatut);
        return ApiResponseDTO.success(updated)
                .message("Statut du projet mis à jour : " + nouveauStatut);
    }

    // Suppression
    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        projetService.deleteById(id);
        return new ApiResponseDTO<>(true, "Projet supprimé avec succès", null);
    }

    // Raccourcis rapides (optionnels mais pratiques dans l'admin)
    @PatchMapping("/{id}/valider")
    public ApiResponseDTO<ProjetDTO> valider(@PathVariable Long id) {
        return changerStatut(id, StatutProjet.VALIDE);
    }

    @PatchMapping("/{id}/rejeter")
    public ApiResponseDTO<ProjetDTO> rejeter(@PathVariable Long id) {
        return changerStatut(id, StatutProjet.REJETE);
    }

    @PatchMapping("/{id}/soumettre")
    public ApiResponseDTO<ProjetDTO> remettreEnSoumission(@PathVariable Long id) {
        return changerStatut(id, StatutProjet.SOUMIS);
    }

}