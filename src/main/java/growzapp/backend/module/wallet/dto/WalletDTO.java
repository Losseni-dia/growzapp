package growzapp.backend.module.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

import growzapp.backend.module.wallet.model.Wallet;

@Schema(description = "Représentation du portefeuille (wallet) d'un utilisateur ou d'un projet")
public record WalletDTO(
        @Schema(description = "Identifiant unique du wallet", example = "1")
        Long id,

        @Schema(description = "Solde disponible pour investir ou transférer", type = "number", format = "double", example = "1500.00")
        BigDecimal soldeDisponible,

        @Schema(description = "Solde bloqué en attente de validation (retrait en cours)", type = "number", format = "double", example = "200.00")
        BigDecimal soldeBloque,

        @Schema(description = "Solde prêt à être retiré vers un compte externe", type = "number", format = "double", example = "300.00")
        BigDecimal soldeRetirable
) {
    public WalletDTO(Wallet wallet) {
        this(
                wallet.getId(),
                wallet.getSoldeDisponible(),
                wallet.getSoldeBloque(),
                wallet.getSoldeRetirable() != null ? wallet.getSoldeRetirable() : BigDecimal.ZERO);
    }
}
