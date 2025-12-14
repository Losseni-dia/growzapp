package growzapp.backend.service;

import growzapp.backend.model.entite.ExchangeRate;
import growzapp.backend.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CurrencyService {
    private final ExchangeRateRepository repository;

    @PostConstruct // S'exécute au démarrage de l'app
    public void initDefaultRates() {
        if (repository.count() == 0) {
            saveRate("EUR", BigDecimal.ONE); // Monnaie pivot
            saveRate("XOF", new BigDecimal("655.957"));
            saveRate("USD", new BigDecimal("1.09"));
        }
    }

    private void saveRate(String code, BigDecimal rate) {
        ExchangeRate er = new ExchangeRate();
        er.setCurrencyCode(code);
        er.setRateToBase(rate);
        er.setLastUpdated(LocalDateTime.now());
        repository.save(er);
    }
}