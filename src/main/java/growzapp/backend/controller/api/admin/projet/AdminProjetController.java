package growzapp.backend.controller.api.admin.projet;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.module.projet.dto.ProjetCreateDTO;
import growzapp.backend.module.projet.dto.ProjetDTO;
import growzapp.backend.module.projet.mapper.ProjetMapper;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.service.FileUploadService;
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
    private final ProjetMapper projetMapper; // Injecté pour les conversions
    private final FileUploadService fileUploadService;
    private final ObjectMapper objectMapper;

    // Liste complète
    @GetMapping
    public ApiResponseDTO<List<ProjetDTO>> getAll(@RequestParam(required = false) String search) {
        List<Projet> entities = projetService.getAllAdmin(search);
        List<ProjetDTO> dtos = projetMapper.toDtoList(entities);
        return ApiResponseDTO.success(dtos)
                .message(dtos.isEmpty() ? "Aucun projet trouvé" : "Projets récupérés avec succès");
    }

    // Détail d'un projet
    @GetMapping("/{id}")
    public ApiResponseDTO<ProjetDTO> getOne(@PathVariable Long id) {
        Projet entity = projetService.getById(id);
        return ApiResponseDTO.success(projetMapper.toDto(entity));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponseDTO<ProjetDTO> update(
            @PathVariable Long id,
            @RequestPart("projet") ProjetCreateDTO dto, // Spring va maintenant reconnaître le JSON grâce au Blob
            @RequestPart(value = "poster", required = false) MultipartFile poster) {
        Projet saved = projetService.updateFull(id, dto, poster);
        return ApiResponseDTO.success(projetMapper.toDto(saved));
    }

    @PatchMapping("/{id}/statut")
    public ApiResponseDTO<ProjetDTO> changerStatut(@PathVariable Long id, @RequestBody StatutProjet nouveauStatut) {
        Projet updated = projetService.changerStatut(id, nouveauStatut);
        return ApiResponseDTO.success(projetMapper.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ApiResponseDTO<Void> delete(@PathVariable Long id) {
        projetService.deleteById(id);
        return new ApiResponseDTO<>(true, "Projet supprimé avec succès", null);
    }

    // Raccourcis
    @PatchMapping("/{id}/valider")
    public ApiResponseDTO<ProjetDTO> valider(@PathVariable Long id) {
        return changerStatut(id, StatutProjet.VALIDE);
    }

    // --- LOGIQUE INTERNE DE MAPPING (Pour éviter de polluer le service) ---
    private void updateEntityFromNode(Projet p, JsonNode node) {
        if (node.has("libelle"))
            p.setLibelle(node.get("libelle").asText());
        if (node.has("description"))
            p.setDescription(node.get("description").asText());
        if (node.has("objectifFinancement"))
            p.setObjectifFinancement(node.get("objectifFinancement").decimalValue());
        if (node.has("prixUnePart"))
            p.setPrixUnePart(node.get("prixUnePart").decimalValue());
        if (node.has("partsDisponible"))
            p.setPartsDisponible(node.get("partsDisponible").asInt());
        if (node.has("roiProjete"))
            p.setRoiProjete(node.get("roiProjete").asDouble());
        // Ajoute ici les autres champs si nécessaire...
    }
}