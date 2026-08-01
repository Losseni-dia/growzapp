package growzapp.backend.module.kyc.dto;

import growzapp.backend.module.kyc.enums.KycStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Ligne d'historique d'un dossier KYC déjà traité (validé ou rejeté)")
public record KycHistoriqueDTO(
        @Schema(example = "42") Long id,
        @Schema(example = "John") String prenom,
        @Schema(example = "Doe") String nom,
        @Schema(example = "john.doe@example.com") String email,
        @Schema(description = "Statut final du dossier", example = "VALIDE") KycStatus kycStatus,
        @Schema(description = "Numéro de la pièce d'identité") String kycNumeroPiece,
        @Schema(description = "Date d'expiration de la pièce d'identité") LocalDate kycDateExpiration,
        @Schema(description = "Date de la décision (validation)") LocalDateTime kycDateValidation,
        @Schema(description = "Motif du rejet, le cas échéant") String kycCommentaireRejet
) {
}
