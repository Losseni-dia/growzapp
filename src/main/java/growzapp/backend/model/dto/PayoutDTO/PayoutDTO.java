package growzapp.backend.model.dto.PayoutDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PayoutDTO(
        Long id,
        BigDecimal montant,
        String type,
        String statut,
        String phone,
        LocalDateTime createdAt,
        String invoiceUrl) {
}
