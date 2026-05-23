package growzapp.backend.module.investissement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Corps alternatif de création d'un investissement (format record)")
public record InvestissementRequestDto(
        @Schema(description = "Identifiant du projet à financer", example = "7")
        Long projetId,

        @Schema(description = "Nombre de parts à acquérir", example = "5")
        int nombrePartsPris,

        @Schema(description = "Frais de transaction en pourcentage (optionnel)", type = "number", format = "double", example = "1.5")
        Double frais,

        @Schema(description = "Identifiant de l'investisseur — injecté par le backend", hidden = true)
        Long investisseurId
) {
}
