package growzapp.backend.model.dto.investisementDTO;


import lombok.Data;

@Data
public class InvestissementCreateDTO {
    private Long projetId;
    private Integer nombrePartsPris;
    private Double frais; // optionnel
    // Tu peux ajouter moyen de paiement, etc.

    // Injecté automatiquement côté backend
    private Long investisseurId;
}