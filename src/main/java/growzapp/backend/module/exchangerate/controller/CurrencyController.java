package growzapp.backend.module.exchangerate.controller;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.exchangerate.dto.ExchangeRateDTO;
import growzapp.backend.module.exchangerate.model.ExchangeRate;
import growzapp.backend.module.exchangerate.repository.ExchangeRateRepository;
import growzapp.backend.module.exchangerate.service.CurrencyService;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/currencies", "/api/currencies"})
@RequiredArgsConstructor
@Tag(name = "Devises", description = "Taux de change utilisés sur la plateforme Growzapp")
public class CurrencyController {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyService currencyService;

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

    // ── ADMIN : LISTER TOUTES LES DEVISES (détail complet) ──────────────────
    @GetMapping("/admin/liste")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Lister toutes les devises avec leur taux et date de mise à jour", tags = { "Devises" })
    public ResponseEntity<ApiResponseDTO<List<ExchangeRateDTO>>> listerToutesLesDevises() {
        List<ExchangeRateDTO> devises = exchangeRateRepository.findAll().stream()
                .sorted(Comparator.comparing(ExchangeRate::getCurrencyCode))
                .map(r -> new ExchangeRateDTO(r.getCurrencyCode(), r.getRateToBase(), r.getLastUpdated()))
                .toList();
        return ResponseEntity.ok(ApiResponseDTO.success(devises));
    }

    // ── ADMIN : CRÉER OU MODIFIER UNE DEVISE ────────────────────────────────
    @PutMapping("/admin/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Créer ou modifier le taux de change d'une devise", description = "Le taux est exprimé par rapport à l'EUR (EUR = 1.0). Si la devise n'existe pas encore, elle est créée.", tags = {
            "Devises" })
    public ResponseEntity<ApiResponseDTO<String>> creerOuModifierDevise(
            @Parameter(description = "Code ISO 4217 de la devise", example = "XOF") @PathVariable String code,
            @RequestBody Map<String, BigDecimal> body) {
        try {
            currencyService.updateRate(code, body.get("rateToBase"));
            return ResponseEntity.ok(ApiResponseDTO.<String>success(null).message("Devise enregistrée"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }

    // ── ADMIN : SUPPRIMER UNE DEVISE ─────────────────────────────────────────
    @DeleteMapping("/admin/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Supprimer une devise", description = "La devise pivot (EUR) ne peut pas être supprimée.", tags = { "Devises" })
    public ResponseEntity<ApiResponseDTO<String>> supprimerDevise(@PathVariable String code) {
        try {
            currencyService.deleteRate(code);
            return ResponseEntity.ok(ApiResponseDTO.<String>success(null).message("Devise supprimée"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        }
    }
}