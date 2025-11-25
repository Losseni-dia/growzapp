// src/main/java/growzapp/backend/model/dto/walletDTOs/WalletDTO.java

package growzapp.backend.model.dto.walletDTOs;

import growzapp.backend.model.entite.Wallet;
import java.math.BigDecimal;

public record WalletDTO(
        Long id,
        BigDecimal soldeDisponible,
        BigDecimal soldeBloque,
        BigDecimal soldeRetirable // CHAMP OBLIGATOIRE
) {
    // CE CONSTRUCTEUR EST LA CLÉ
    public WalletDTO(Wallet wallet) {
        this(
                wallet.getId(),
                wallet.getSoldeDisponible(),
                wallet.getSoldeBloque(),
                wallet.getSoldeRetirable() != null ? wallet.getSoldeRetirable() : BigDecimal.ZERO);
    }
}