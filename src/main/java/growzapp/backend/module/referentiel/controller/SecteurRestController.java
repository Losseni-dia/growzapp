package growzapp.backend.module.referentiel.controller;

import growzapp.backend.module.referentiel.dto.SecteurDTO;
import growzapp.backend.module.referentiel.mapper.ReferentielMapper;
import growzapp.backend.module.referentiel.model.Secteur;
import growzapp.backend.module.referentiel.service.SecteurService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/secteurs")
@RequiredArgsConstructor
@Tag(name = "Référentiels", description = "Données de référence : pays, localités, localisations, langues et secteurs d'activité")
public class SecteurRestController {

    private final SecteurService secteurService;
    private final ReferentielMapper referentielMapper;

    @GetMapping
    @Operation(summary = "Lister tous les secteurs d'activité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Liste des secteurs",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<List<SecteurDTO>> getAll() {
        return ApiResponseDTO.success(referentielMapper.toSecteurDtoList(secteurService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un secteur d'activité", tags = {"Référentiels"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secteur trouvé",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Secteur introuvable",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<SecteurDTO> getById(
            @Parameter(description = "Identifiant du secteur", example = "2", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(referentielMapper.toSecteurDto(secteurService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Créer un secteur d'activité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Secteur créé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<SecteurDTO> create(@RequestBody SecteurDTO dto) {
        Secteur entity = referentielMapper.toSecteurEntity(dto);
        return ApiResponseDTO.success(referentielMapper.toSecteurDto(secteurService.save(entity)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un secteur d'activité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Secteur mis à jour",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<SecteurDTO> update(
            @Parameter(description = "Identifiant du secteur", example = "2", required = true)
            @PathVariable Long id,
            @RequestBody SecteurDTO dto) {
        Secteur entity = referentielMapper.toSecteurEntity(dto);
        entity.setId(id);
        return ApiResponseDTO.success(referentielMapper.toSecteurDto(secteurService.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un secteur d'activité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Secteur supprimé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant du secteur", example = "2", required = true)
            @PathVariable Long id) {
        secteurService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}
