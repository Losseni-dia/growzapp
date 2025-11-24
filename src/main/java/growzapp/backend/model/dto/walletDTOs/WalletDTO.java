package growzapp.backend.model.dto.walletDTOs;

import java.math.BigDecimal;


public record WalletDTO(
        Long id,
        BigDecimal soldeDisponible,
        BigDecimal soldeBloque,
        BigDecimal soldeTotal) {

            
    public WalletDTO(growzapp.backend.model.entite.Wallet w) {
        this(w.getId(), w.getSoldeDisponible(), w.getSoldeBloque(), w.getSoldeTotal());
    }
}