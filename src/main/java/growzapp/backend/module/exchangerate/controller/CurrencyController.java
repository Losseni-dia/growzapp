package growzapp.backend.module.exchangerate.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.exchangerate.model.ExchangeRate;
import growzapp.backend.module.exchangerate.repository.ExchangeRateRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/currencies", "/api/currencies"})
@RequiredArgsConstructor
@Tag(name = "Devises", description = "Taux de change utilisés sur la plateforme Growzapp")
public class CurrencyController {

    private final ExchangeRateRepository exchangeRateRepository;

    @GetMapping("/rates")
    @Operation(summary = "Taux de change", description = "Retourne les taux de change fixes depuis la base de données, "
            +
            "exprimés en valeur relative à l'Euro (EUR = 1.0). " +
            "Taux fixes pour garantir la stabilité des investissements.", tags = { "Devises" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Taux de change disponibles", content = @Content(mediaType = "application/json", schema = @Schema(example = "{\"EUR\": 1.0, \"XOF\": 655.957, \"USD\": 1.08}")))
    })
    public ResponseEntity<ApiResponseDTO<Map<String, Double>>> getExchangeRates() {
        List<ExchangeRate> allRates = exchangeRateRepository.findAll();

        Map<String, Double> rates = new LinkedHashMap<>();
        allRates.forEach(rate -> rates.put(rate.getCurrencyCode(), rate.getRateToBase().doubleValue()));

        // Fallback si la base est vide
        if (rates.isEmpty()) {
            rates.put("EUR", 1.0);
            rates.put("XOF", 655.957);
            rates.put("XAF", 655.957);
            rates.put("USD", 1.08);
            rates.put("GBP", 0.86);
            rates.put("MAD", 10.85);
            rates.put("GHS", 14.50);
            rates.put("KES", 140.00);
            rates.put("NGN", 1650.00);
            rates.put("GNF", 9300.00);
        }

        return ResponseEntity.ok(ApiResponseDTO.success(rates));
    }
}