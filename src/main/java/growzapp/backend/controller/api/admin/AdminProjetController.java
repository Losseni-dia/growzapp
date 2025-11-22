package growzapp.backend.controller.api.admin;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/projets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjetController {

    private final ProjetService projetService;

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
    @PutMapping(value = "/{id}", consumes = { "multipart/form-data" })
    public ApiResponseDTO<ProjetDTO> update(
            @PathVariable Long id,
            @RequestPart("projet") @Valid ProjetDTO dto,
            @RequestPart(value = "poster", required = false) MultipartFile poster) {

        MultipartFile[] files = poster != null ? new MultipartFile[] { poster } : null;

        ProjetDTO updated = projetService.updateProjet(id, dto, files); // PLUS DE SAVE()

        return ApiResponseDTO.success(updated).message("Projet mis à jour avec succès");
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