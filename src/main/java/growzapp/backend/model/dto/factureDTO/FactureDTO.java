// src/main/java/growzapp/backend/model/dto/factureDTO/FactureDTO.java
package growzapp.backend.model.dto.factureDTO;

import growzapp.backend.model.enumeration.StatutFacture;

import java.time.LocalDateTime;

public record FactureDTO(
        Long id,
        String numeroFacture,
        Double montantHT, // Montant hors taxes
        Double tva, // Taux ou montant TVA (0.0 par défaut)
        Double montantTTC, // Montant total (calculé)
        LocalDateTime dateEmission,
        LocalDateTime datePaiement,
        StatutFacture statut,

        // Relations utiles côté frontend
        Long dividendeId,
        Long investisseurId,
        String investisseurNom,

        // URL du fichier PDF stocké (pour affichage ou téléchargement direct si besoin)
        String fichierUrl

// Plus besoin d'ajouter autre chose ici
) {
}