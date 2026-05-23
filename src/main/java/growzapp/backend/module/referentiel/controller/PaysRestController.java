package growzapp.backend.module.referentiel.controller;

import growzapp.backend.module.referentiel.dto.PaysDTO;
import growzapp.backend.module.referentiel.mapper.ReferentielMapper;
import growzapp.backend.module.referentiel.model.Pays;
import growzapp.backend.module.referentiel.service.PaysService;
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
@RequestMapping("/api/pays")
@RequiredArgsConstructor
@Tag(name = "Référentiels", description = "Données de référence : pays, localités, localisations, langues et secteurs d'activité")
public class PaysRestController {

    private final PaysService paysService;
    private final ReferentielMapper referentielMapper;

    @GetMapping
    @Operation(summary = "Lister tous les pays", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Liste des pays",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<List<PaysDTO>> getAll() {
        return ApiResponseDTO.success(referentielMapper.toPaysDtoList(paysService.getAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un pays", tags = {"Référentiels"})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pays trouvé",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Pays introuvable",
                    content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<PaysDTO> getById(
            @Parameter(description = "Identifiant du pays", example = "1", required = true)
            @PathVariable Long id) {
        return ApiResponseDTO.success(referentielMapper.toPaysDto(paysService.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Créer un pays", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Pays créé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<PaysDTO> create(@RequestBody PaysDTO dto) {
        Pays entity = referentielMapper.toPaysEntity(dto);
        return ApiResponseDTO.success(referentielMapper.toPaysDto(paysService.save(entity)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un pays", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Pays mis à jour",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<PaysDTO> update(
            @Parameter(description = "Identifiant du pays", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody PaysDTO dto) {
        Pays entity = referentielMapper.toPaysEntity(dto);
        entity.setId(id);
        return ApiResponseDTO.success(referentielMapper.toPaysDto(paysService.save(entity)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un pays", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Pays supprimé",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<Void> delete(
            @Parameter(description = "Identifiant du pays", example = "1", required = true)
            @PathVariable Long id) {
        paysService.deleteById(id);
        return ApiResponseDTO.success(null);
    }
}
