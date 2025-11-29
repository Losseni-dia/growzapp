package growzapp.backend.model.dto.walletDTOs;

// src/main/java/growzapp/backend/model/dto/wallet/DepotRequest.java

import java.math.BigDecimal;

public record DepotRequest(BigDecimal montant) {

    public DepotRequest{if(montant==null){throw new IllegalArgumentException("Le montant est requis");}if(montant.compareTo(BigDecimal.ZERO)<=0){throw new IllegalArgumentException("Le montant doit être strictement positif");}if(montant.scale()>2){throw new IllegalArgumentException("Maximum 2 décimales autorisées");}
    // Optionnel : dépôt minimum
    if(montant.compareTo(new BigDecimal("10.00"))<0){throw new IllegalArgumentException("Le dépôt minimum est de 10,00 €");}}
}