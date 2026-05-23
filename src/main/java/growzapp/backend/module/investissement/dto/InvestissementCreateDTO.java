package growzapp.backend.module.investissement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Données requises pour créer ou modifier un investissement")
public class InvestissementCreateDTO {

    @Schema(description = "Identifiant du projet dans lequel investir", example = "7")
    private Long projetId;

    @Schema(description = "Nombre de parts à acquérir", example = "5")
    private Integer nombrePartsPris;

    @Schema(description = "Frais de transaction applicables en pourcentage (optionnel, calculé par le backend si absent)", type = "number", format = "double", example = "1.5")
    private Double frais;

    @Schema(description = "Identifiant de l'investisseur — injecté automatiquement par le backend, ne pas renseigner", hidden = true)
    private Long investisseurId;
}
