package growzapp.backend.module.referentiel.controller;

import growzapp.backend.module.referentiel.dto.LocalisationDTO;
import growzapp.backend.module.referentiel.mapper.ReferentielMapper;
import growzapp.backend.module.referentiel.model.Localisation;
import growzapp.backend.module.referentiel.repository.LocaliteRepository;
import growzapp.backend.module.referentiel.service.LocalisationService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/localisations")
@RequiredArgsConstructor
@Tag(name = "Référentiels", description = "Données de référence : pays, localités, localisations, langues et secteurs d'activité")
public class LocalisationRestController {

    private final LocalisationService localisationService;
    private final LocaliteRepository localiteRepository;
    private final ReferentielMapper referentielMapper;

    @GetMapping
    @Operation(summary = "Lister toutes les localisations", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Liste des localisations",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<List<LocalisationDTO>> getAll() {
        return ApiResponseDTO.success(referentielMapper.toLocalisationDtoList(localisationService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une localisation", tags = {"Référentiels"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localisation trouvée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Localisation introuvable",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<LocalisationDTO> getById(
            @Parameter(description = "Identifiant de la localisation", example = "3", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(referentielMapper.toLocalisationDto(localisationService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Créer une localisation", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Localisation créée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<LocalisationDTO> create(@RequestBody LocalisationDTO dto) {
        Localisation entity = referentielMapper.toLocalisationEntity(dto);
        Localisation saved = localisationService.save(entity, dto.localiteNom());
        return ApiResponseDTO.success(referentielMapper.toLocalisationDto(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une localisation", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Localisation supprimée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant de la localisation", example = "3", required = true)
            @PathVariable Long id) {
        localisationService.deleteById(id);
        return ApiResponseDTO.success(null);
    }

    @Hidden
    @GetMapping("/localites/{localiteId}/create-localisation")
    public String createFormWithLocalite(@PathVariable Long localiteId, Model model) {
        growzapp.backend.module.referentiel.model.Localite localite = localiteRepository.findById(localiteId)
                .orElseThrow(() -> new RuntimeException("Localité non trouvée"));

        LocalisationDTO dto = new LocalisationDTO(
                null, null, null, null, null, null,
                null, null, null, null,
                localite.getNom(), localiteId, null, List.of()
        );
        model.addAttribute("localisation", dto);
        model.addAttribute("title", "Créer une localisation à " + localite.getNom());
        return "localisation/form";
    }
}
