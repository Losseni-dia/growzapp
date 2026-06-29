package growzapp.backend.module.exchangerate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import growzapp.backend.module.exchangerate.model.ExchangeRate;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {

    Optional<ExchangeRate> findByCurrencyCode(String currencyCode);
}