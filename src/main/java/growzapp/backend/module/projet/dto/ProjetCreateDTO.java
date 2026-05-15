package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "DTO pour la création d'un nouveau projet d'investissement")
public record ProjetCreateDTO(
        @Schema(example = "Résidence Horizon", description = "Titre du projet") String libelle,

        @Schema(example = "Construction d'un complexe immobilier de 10 appartements.") String description,

        @Schema(example = "Immobilier", description = "Nom du secteur d'activité") String secteurNom,

        @Schema(example = "Abidjan", description = "Ville ou localité") String localiteNom,

        @Schema(example = "Côte d'Ivoire") String paysNom,

        @Schema(example = "50000000", description = "Montant total à collecter") BigDecimal objectifFinancement,

        @Schema(example = "50000", description = "Prix d'une seule part") BigDecimal prixUnePart,

        @Schema(example = "1000", description = "Nombre total de parts émises") int partsDisponible,

        @Schema(example = "12.5", description = "Retour sur investissement estimé (%)") double roiProjete,

        @Schema(example = "100000000", description = "Valorisation totale du projet") BigDecimal valuation,

        @Schema(example = "36", description = "Durée du projet en mois") Integer dureeMois,

        @Schema(example = "XOF", description = "Code de la devise (ISO 4217)") String currencyCode,

        @Schema(example = "SOUMIS", description = "Statut initial du projet") String statutProjet,

        @Schema(example = "2026-06-01") LocalDate dateDebut,

        @Schema(example = "2026-12-31") LocalDate dateFin,

        @Schema(hidden = true) // On cache ce champ au client lors de la création
        LocalDateTime certifiedAt) {
}