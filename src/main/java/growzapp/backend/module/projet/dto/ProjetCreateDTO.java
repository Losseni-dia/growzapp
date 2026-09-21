package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO pour la création d'un nouveau projet d'investissement")
public record ProjetCreateDTO(

                @NotBlank(message = "Le titre du projet est obligatoire") @Size(min = 3, max = 150, message = "Le titre doit contenir entre 3 et 150 caractères") @Schema(example = "Résidence Horizon", description = "Titre du projet") String libelle,

                @NotBlank(message = "La description est obligatoire") @Size(min = 20, max = 5000, message = "La description doit contenir entre 20 et 5000 caractères") @Schema(example = "Construction d'un complexe immobilier de 10 appartements.") String description,

                @NotBlank(message = "Le secteur d'activité est obligatoire") @Schema(example = "Immobilier", description = "Nom du secteur d'activité") String secteurNom,

                @NotBlank(message = "La ville ou localité est obligatoire") @Schema(example = "Abidjan", description = "Ville ou localité") String localiteNom,

                @Schema(example = "Côte d'Ivoire") String paysNom,

                @NotNull(message = "L'objectif de financement est obligatoire") @DecimalMin(value = "100000", message = "L'objectif minimum est de 100 000 FCFA") @Schema(example = "50000000", description = "Montant total à collecter") BigDecimal objectifFinancement,

                @NotNull(message = "Le prix d'une part est obligatoire") @DecimalMin(value = "1000", message = "Le prix minimum par part est de 1 000 FCFA") @Schema(example = "50000", description = "Prix d'une seule part") BigDecimal prixUnePart,

                @Min(value = 1, message = "Le nombre de parts doit être d'au moins 1") @Schema(example = "1000", description = "Nombre total de parts émises") int partsDisponible,

                @DecimalMin(value = "0.1", message = "Le ROI doit être supérieur à 0") @DecimalMax(value = "100.0", message = "Le ROI ne peut pas dépasser 100%") @Schema(example = "12.5", description = "Retour sur investissement estimé (%)") double roiProjete,

                @NotNull(message = "La valorisation est obligatoire") @DecimalMin(value = "100000", message = "La valorisation minimum est de 100 000 FCFA") @Schema(example = "100000000", description = "Valorisation totale du projet") BigDecimal valuation,

                @Min(value = 1, message = "La durée minimum est de 1 mois") @Max(value = 240, message = "La durée maximum est de 240 mois") @Schema(example = "36", description = "Durée du projet en mois — laisser vide pour une durée indéterminée") Integer dureeMois,

                @Schema(example = "XOF", description = "Code de la devise (ISO 4217)") String currencyCode,

                @Schema(example = "SOUMIS", description = "Statut initial du projet") String statutProjet,

               @NotNull(message = "La date de début est obligatoire") @Schema(example = "2026-06-01") LocalDate dateDebut,

                @NotNull(message = "La date de fin est obligatoire") @Schema(example = "2026-12-31") LocalDate dateFin,

                @DecimalMin(value = "-90.0", message = "Latitude invalide") @DecimalMax(value = "90.0", message = "Latitude invalide") @Schema(example = "5.359951", description = "Latitude exacte du site du projet") BigDecimal latitude,

                @DecimalMin(value = "-180.0", message = "Longitude invalide") @DecimalMax(value = "180.0", message = "Longitude invalide") @Schema(example = "-4.008256", description = "Longitude exacte du site du projet") BigDecimal longitude,

                @Schema(example = "Rue du Commerce, Yamoussoukro", description = "Adresse lisible du site (géocodage inverse ou saisie manuelle)") String adresse) {
}