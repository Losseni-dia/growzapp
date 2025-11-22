package growzapp.backend.model.dto.factureDTO;

import growzapp.backend.model.enumeration.StatutFacture;
import java.time.LocalDateTime;

public record FactureDTO(
                Long id,
                String numeroFacture,
                Double montantHT, // ← double (obligatoire)
                Double tva, // ← double (défaut 0.0)
                Double montantTTC, // ← Double (calculé, peut être null)
                LocalDateTime dateEmission,
                LocalDateTime datePaiement,
                StatutFacture statut,

                Long dividendeId,
                Long investisseurId,
                String investisseurNom,
                String fichierUrl) {
}