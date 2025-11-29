package growzapp.backend.controller.api.admin;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.dividendeDTO.PayerDividendeGlobalRequest;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.WalletRepository;
import growzapp.backend.service.DividendeService;
import growzapp.backend.service.FileUploadService;
import growzapp.backend.service.ProjetService;
import growzapp.backend.service.WalletService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/projets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProjetController {

    private final ProjetService projetService;
    private final FileUploadService fileUploadService;
    private final DividendeService dividendeService;
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





    @PostMapping("/{projetId}/payer-dividendes")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponseDTO<String>> payerDividendesProrata(
            @PathVariable Long projetId,
            @Valid @RequestBody PayerDividendeGlobalRequest request) {

        dividendeService.payerDividendesProjetProrata(
            request.projetId(),
            request.montantTotal(),
            request.motif(),
            request.periode()
        );

        return ResponseEntity.ok(ApiResponseDTO.success("Dividendes distribués")
            .message("Montant total de " + request.montantTotal() + " FCFA distribué au prorata"));
    }

     

        


        
}