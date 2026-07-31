package growzapp.backend.module.settings.controller;

import growzapp.backend.module.settings.dto.SiteSettingDTO;
import growzapp.backend.module.settings.service.SiteSettingService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/site-settings", "/api/site-settings"})
@RequiredArgsConstructor
@Tag(name = "Paramètres du site", description = "Configuration générale de la plateforme (langue par défaut pour les visiteurs non connectés)")
public class SiteSettingController {

    private final SiteSettingService siteSettingService;

    @GetMapping
    @Operation(
        summary = "Lire la configuration du site",
        description = "Retourne la langue par défaut affichée aux visiteurs non connectés. Endpoint public.",
        tags = {"Paramètres du site"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<SiteSettingDTO> get() {
        return ApiResponseDTO.success(siteSettingService.get());
    }

    @PutMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "[Admin] Modifier la langue par défaut du site",
        description = "Change la langue affichée aux visiteurs non connectés. Réservé aux administrateurs.",
        tags = {"Paramètres du site"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Configuration mise à jour",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Langue non supportée",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<SiteSettingDTO> update(@RequestBody SiteSettingDTO dto) {
        return ApiResponseDTO.success(
                siteSettingService.updateDefaultLanguage(dto.defaultLanguage()));
    }
}
