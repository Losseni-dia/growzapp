package growzapp.backend.module.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Taux de change d'une devise par rapport à la devise pivot (EUR = 1.0)")
public record ExchangeRateDTO(
        @Schema(example = "XOF") String currencyCode,
        @Schema(example = "655.957") BigDecimal rateToBase,
        LocalDateTime lastUpdated) {
}
