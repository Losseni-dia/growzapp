package growzapp.backend.controller.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currencies")
public class CurrencyController {

    @GetMapping("/rates")
    public Map<String, Double> getExchangeRates() {
        // Dans une version finale, vous pourriez appeler une API externe (Fixer.io,
        // etc.)
        // Ici, on définit des taux fixes par rapport à votre monnaie pivot (ex: Euro)
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 1.0);
        rates.put("XOF", 655.957);
        rates.put("USD", 1.08);
        return rates;
    }
}