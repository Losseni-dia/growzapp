package growzapp.backend.module.projet.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjetCreateDTO(
                String libelle,
                String description,
                String secteurNom,
                String localiteNom,
                String paysNom,
                BigDecimal objectifFinancement,
                BigDecimal prixUnePart,
                int partsDisponible,
                double roiProjete,
                BigDecimal valuation,
                Integer dureeMois,
                String currencyCode,
                String statutProjet,
                LocalDate dateDebut,
                LocalDate dateFin,
                LocalDateTime certifiedAt) {
}