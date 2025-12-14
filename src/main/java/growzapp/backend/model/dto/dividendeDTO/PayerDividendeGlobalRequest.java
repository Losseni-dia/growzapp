// src/main/java/growzapp/backend/model/dto/dividendeDTO/PayerDividendeGlobalRequest.java

package growzapp.backend.model.dto.dividendeDTO;

import jakarta.validation.constraints.*;

public record PayerDividendeGlobalRequest(

        @NotNull(message = "Le montant total est obligatoire") @Min(value = 1, message = "Le montant doit être supérieur à 0")
        // @Positive marche aussi si tu préfères
        Double montantTotal,

        @NotBlank(message = "Le motif est obligatoire") String motif,

        @NotBlank(message = "La période est obligatoire (ex: Q4 2025, Décembre 2025...)") String periode

// ← projetId SUPPRIMÉ : il vient déjà du @PathVariable dans l'URL
) {
}