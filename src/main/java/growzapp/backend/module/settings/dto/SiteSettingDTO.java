package growzapp.backend.module.settings.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Configuration générale du site")
public record SiteSettingDTO(
        @Schema(description = "Langue affichée aux visiteurs non connectés", example = "fr", allowableValues = {"fr", "en", "es"})
        String defaultLanguage
) {
}
