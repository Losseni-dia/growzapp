package growzapp.backend.module.projet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO pour l'enregistrement/mise à jour d'un brouillon de projet — tous les
 * champs sont optionnels puisque le porteur peut sauvegarder son formulaire
 * à n'importe quelle étape de saisie. La validation complète n'intervient
 * qu'au moment de la soumission (ProjetService.soumettreBrouillon), pas ici.
 * Les contraintes de format ci-dessous (@Size, @DecimalMin, @Min/@Max) ne
 * s'appliquent qu'aux valeurs effectivement renseignées — null les ignore.
 */
@Schema(description = "DTO pour l'enregistrement d'un brouillon de projet (tous les champs sont optionnels)")
public record ProjetBrouillonDTO(

                @Size(max = 150, message = "Le titre ne doit pas dépasser 150 caractères") String libelle,

                @Size(max = 5000, message = "La description ne doit pas dépasser 5000 caractères") String description,

                String secteurNom,

                String localiteNom,

                String paysNom,

                @DecimalMin(value = "0", message = "L'objectif de financement doit être positif") BigDecimal objectifFinancement,

                @DecimalMin(value = "0", message = "Le prix d'une part doit être positif") BigDecimal prixUnePart,

                @Min(value = 0, message = "Le nombre de parts ne peut pas être négatif") Integer partsDisponible,

                @DecimalMin(value = "0", message = "Le ROI ne peut pas être négatif") @DecimalMax(value = "100.0", message = "Le ROI ne peut pas dépasser 100%") Double roiProjete,

                @DecimalMin(value = "0", message = "La valorisation doit être positive") BigDecimal valuation,

                @Min(value = 1, message = "La durée minimum est de 1 mois") @Max(value = 240, message = "La durée maximum est de 240 mois") Integer dureeMois,

                String currencyCode,

                LocalDate dateDebut,

                LocalDate dateFin) {
}
