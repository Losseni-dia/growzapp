// src/main/java/growzapp/backend/model/dto/dividendeDTO/PayerDividendeGlobalRequest.java
package growzapp.backend.model.dto.dividendeDTO;

import jakarta.validation.constraints.*;

public record PayerDividendeGlobalRequest(
                @NotNull Long projetId,
                @NotNull @Min(1000) Double montantTotal,
                @NotBlank String motif,
                String periode) {
}