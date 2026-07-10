package growzapp.backend.module.exchangerate.service;

import growzapp.backend.module.exchangerate.model.ExchangeRate;
import growzapp.backend.module.exchangerate.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final ExchangeRateRepository repository;

    @PostConstruct
    public void initDefaultRates() {
        if (repository.count() == 0) {

            // ── Monnaie pivot ─────────────────────────────────────────
            saveRate("EUR", BigDecimal.ONE);

            // ── Zone Franc CFA (taux fixe légal garanti par la France) ─
            saveRate("XOF", new BigDecimal("655.957")); // Franc CFA Ouest
            saveRate("XAF", new BigDecimal("655.957")); // Franc CFA Centre

            // ── Grandes devises mondiales ──────────────────────────────
            saveRate("USD", new BigDecimal("1.08"));
            saveRate("GBP", new BigDecimal("0.86"));

            // ── Devises africaines stables ─────────────────────────────
            saveRate("MAD", new BigDecimal("10.85"));
            saveRate("GHS", new BigDecimal("14.50"));
            saveRate("KES", new BigDecimal("140.00"));
            saveRate("NGN", new BigDecimal("1650.00"));
            saveRate("GNF", new BigDecimal("9300.00"));
        }
    }

    private void saveRate(String code, BigDecimal rate) {
        ExchangeRate er = new ExchangeRate();
        er.setCurrencyCode(code);
        er.setRateToBase(rate);
        er.setLastUpdated(LocalDateTime.now());
        repository.save(er);
    }

    public void updateRate(String code, BigDecimal newRate) {
        ExchangeRate er = repository.findById(code)
                .orElse(new ExchangeRate());
        er.setCurrencyCode(code);
        er.setRateToBase(newRate);
        er.setLastUpdated(LocalDateTime.now());
        repository.save(er);
    }
}