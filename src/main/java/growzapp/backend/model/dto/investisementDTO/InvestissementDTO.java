package growzapp.backend.model.dto.investisementDTO;

import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record InvestissementDTO(
                Long id,
                int nombrePartsPris,
                LocalDateTime date,
                double valeurPartsPrisEnPourcent, // % equity
                double frais,
                StatutPartInvestissement statutPartInvestissement,

                // Relations (IDs)
                Long investisseurId,
                Long projetId,

                // Relations (Noms pour affichage)
                String investisseurNom,
                String projetLibelle,
                double prixUnePart,

                // === ENRICHISSEMENTS ===
                List<DividendeDTO> dividendes, // Tous les dividendes
                double montantTotalPercu, // Somme des dividendes payés
                double montantTotalPlanifie, // Somme des dividendes planifiés
                double roiRealise, // (montantTotalPercu / valeur initiale) * 100
                int dividendesPayes, // Nombre de dividendes PAYE
                int dividendesPlanifies, // Nombre de dividendes PLANIFIE
                String statutGlobalDividendes // "Tous payés", "En attente", "Partiel"
) {
}