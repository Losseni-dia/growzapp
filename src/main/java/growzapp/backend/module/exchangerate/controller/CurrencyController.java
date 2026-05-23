package growzapp.backend.module.exchangerate.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/currencies")
@Tag(name = "Devises", description = "Taux de change utilisés sur la plateforme Growzapp")
public class CurrencyController {

    @GetMapping("/rates")
    @Operation(
        summary = "Taux de change",
        description = "Retourne les taux de change fixes utilisés par la plateforme, exprimés en valeur relative à l'Euro (EUR = 1.0). Dans une version finale, ces taux pourront être récupérés depuis une API externe (ex: Fixer.io).",
        tags = {"Devises"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Taux de change disponibles",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"EUR\": 1.0, \"XOF\": 655.957, \"USD\": 1.08}")))
    })
    public Map<String, Double> getExchangeRates() {
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 1.0);
        rates.put("XOF", 655.957);
        rates.put("USD", 1.08);
        return rates;
    }
}
