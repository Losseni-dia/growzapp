package growzapp.backend.module.referentiel.controller;

import growzapp.backend.module.referentiel.dto.LangueDTO;
import growzapp.backend.module.referentiel.mapper.ReferentielMapper;
import growzapp.backend.module.referentiel.service.LangueService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/langues")
@RequiredArgsConstructor
@Tag(name = "Référentiels", description = "Données de référence : pays, localités, localisations, langues et secteurs d'activité")
public class LangueRestController {

    private final LangueService langueService;
    private final ReferentielMapper referentielMapper;

    @GetMapping
    @PermitAll
    @Operation(summary = "Lister toutes les langues", description = "Endpoint public, aucune authentification requise.", tags = {"Référentiels"})
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Liste des langues disponibles sur la plateforme",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))))
    public ApiResponseDTO<List<LangueDTO>> getAll() {
        return ApiResponseDTO.success(referentielMapper.toLangueDtoList(langueService.getAll()));
    }
}
