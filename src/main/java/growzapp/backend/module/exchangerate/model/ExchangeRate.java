package growzapp.backend.module.exchangerate.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class ExchangeRate {
    @Id
    private String currencyCode; // ex: "USD", "EUR", "CFA"
    private BigDecimal rateToBase; // Taux par rapport à votre monnaie pivot (ex: Euro)
    private LocalDateTime lastUpdated;
}