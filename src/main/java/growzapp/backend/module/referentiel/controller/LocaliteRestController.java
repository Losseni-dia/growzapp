package growzapp.backend.module.referentiel.controller;

import growzapp.backend.module.referentiel.dto.LocaliteDTO;
import growzapp.backend.module.referentiel.mapper.ReferentielMapper;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.referentiel.service.LocaliteService;
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
@RequestMapping("/api/localites")
@RequiredArgsConstructor
@Tag(name = "Référentiels", description = "Données de référence : pays, localités, localisations, langues et secteurs d'activité")
public class LocaliteRestController {

    private final LocaliteService localiteService;
    private final ReferentielMapper referentielMapper;

    @GetMapping
    @Operation(summary = "Lister toutes les localités", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Liste des localités",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<List<LocaliteDTO>> getAll() {
        return ApiResponseDTO.success(referentielMapper.toLocaliteDtoList(localiteService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une localité", tags = {"Référentiels"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Localité trouvée",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Localité introuvable",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<LocaliteDTO> getById(
            @Parameter(description = "Identifiant de la localité", example = "1", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(referentielMapper.toLocaliteDto(localiteService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Créer une localité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Localité créée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<LocaliteDTO> create(@RequestBody LocaliteDTO dto) {
        Localite entity = referentielMapper.toLocaliteEntity(dto);
        Localite saved = localiteService.save(entity, dto.paysNom());
        return ApiResponseDTO.success(referentielMapper.toLocaliteDto(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une localité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Localité mise à jour",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<LocaliteDTO> update(
            @Parameter(description = "Identifiant de la localité", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody LocaliteDTO dto) {
        Localite entity = referentielMapper.toLocaliteEntity(dto);
        entity.setId(id);
        Localite saved = localiteService.save(entity, dto.paysNom());
        return ApiResponseDTO.success(referentielMapper.toLocaliteDto(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une localité", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Localité supprimée",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant de la localité", example = "1", required = true)
            @PathVariable Long id) {
        localiteService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}
