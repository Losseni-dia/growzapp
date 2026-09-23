package growzapp.backend.module.growzmarket.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

public record CommandeMarketCreateDTO(
        @NotEmpty @Valid List<CommandeMarketLigneCreateDTO> lignes,
        // Doit être coché — preuve que l'acheteur a vu et accepté le point
        // de retrait déclaré sur l'article avant de payer.
        @AssertTrue boolean confirmationLieuRetrait) {
}
