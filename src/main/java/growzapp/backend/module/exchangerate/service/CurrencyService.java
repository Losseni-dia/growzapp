package growzapp.backend.module.exchangerate.service;

import growzapp.backend.module.exchangerate.model.ExchangeRate;
import growzapp.backend.module.exchangerate.repository.ExchangeRateRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CurrencyService {

    private final ExchangeRateRepository repository;

    // ── Devises sans décimales (mêmes règles que le frontend CurrencyContext) ─
    private static final Set<String> NO_DECIMALS = Set.of("XOF", "XAF", "GNF", "NGN", "KES", "GHS");

    // ── Libellé affiché après le montant sur les documents PDF ───────────────
    private static final Map<String, String> LABELS = Map.of(
            "EUR", "€",
            "USD", "$",
            "GBP", "£",
            "XOF", "FCFA",
            "XAF", "FCFA",
            "MAD", "MAD",
            "GHS", "GHS",
            "KES", "KSh",
            "NGN", "₦",
            "GNF", "FG");

    /** Taux (relatif à l'EUR) d'une devise ; XOF (655.957) si code inconnu. */
    public BigDecimal getRate(String code) {
        if (code == null || code.isBlank()) {
            return new BigDecimal("655.957");
        }
        return repository.findById(code.toUpperCase())
                .map(ExchangeRate::getRateToBase)
                .orElse(new BigDecimal("655.957"));
    }

    /** Convertit un montant exprimé en XOF (devise pivot interne) vers la devise cible. */
    public BigDecimal convertFromXOF(BigDecimal amountXOF, String targetCurrency) {
        if (amountXOF == null) {
            return BigDecimal.ZERO;
        }
        String target = (targetCurrency == null || targetCurrency.isBlank()) ? "XOF" : targetCurrency.toUpperCase();
        BigDecimal rateXof = getRate("XOF");
        BigDecimal rateTarget = getRate(target);
        BigDecimal amountInEur = amountXOF.divide(rateXof, 10, RoundingMode.HALF_UP);
        return amountInEur.multiply(rateTarget);
    }

    /** Partie numérique formatée (séparateur d'espace, virgule décimale — style fr-FR) d'un montant converti. */
    public String formatAmountValue(BigDecimal amountXOF, String targetCurrency) {
        String target = (targetCurrency == null || targetCurrency.isBlank()) ? "XOF" : targetCurrency.toUpperCase();
        BigDecimal converted = convertFromXOF(amountXOF, target);
        int decimals = NO_DECIMALS.contains(target) ? 0 : 2;
        converted = converted.setScale(decimals, RoundingMode.HALF_UP);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        String pattern = decimals == 0 ? "#,##0" : "#,##0.00";
        DecimalFormat df = new DecimalFormat(pattern, symbols);
        return df.format(converted);
    }

    /** Libellé/symbole affiché après le montant (ex : "FCFA", "€", "$"). */
    public String getLabel(String currency) {
        String target = (currency == null || currency.isBlank()) ? "XOF" : currency.toUpperCase();
        return LABELS.getOrDefault(target, target);
    }

    /** Montant + libellé, prêt à afficher (ex : "7,62 €"). */
    public String format(BigDecimal amountXOF, String targetCurrency) {
        return formatAmountValue(amountXOF, targetCurrency) + " " + getLabel(targetCurrency);
    }

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
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Le code devise est obligatoire");
        }
        if (newRate == null || newRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Le taux doit être strictement positif");
        }
        String codeNormalise = code.trim().toUpperCase();
        ExchangeRate er = repository.findById(codeNormalise)
                .orElse(new ExchangeRate());
        er.setCurrencyCode(codeNormalise);
        er.setRateToBase(newRate);
        er.setLastUpdated(LocalDateTime.now());
        repository.save(er);
    }

    public void deleteRate(String code) {
        if ("EUR".equalsIgnoreCase(code)) {
            throw new IllegalStateException("Impossible de supprimer la devise pivot (EUR) — toutes les conversions en dépendent.");
        }
        repository.deleteById(code.toUpperCase());
    }
}