package growzapp.backend.model.dto.walletDTOs;



import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// TransactionDTO.java
public record TransactionDTO(
        Long id,
        BigDecimal montant, // ← BigDecimal, pas double
        TypeTransaction type,
        StatutTransaction statut,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        String description,

        Long userId,
        String userPrenom,
        String userNom,
        String userLogin,

        Long destinataireUserId,
        String destinataireNomComplet,
        String destinataireLogin,

        Long expediteurUserId,
        String expediteurNomComplet,
        String expediteurLogin) {
}