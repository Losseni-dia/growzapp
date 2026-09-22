package growzapp.backend.module.fournisseur.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CommandeCreateDTO(
        @NotNull(message = "Le projet est obligatoire") Long projetId,
        @NotNull(message = "Le fournisseur est obligatoire") Long fournisseurId,
        @NotEmpty(message = "La commande doit contenir au moins un article") @Valid List<CommandeLigneCreateDTO> lignes) {
}
