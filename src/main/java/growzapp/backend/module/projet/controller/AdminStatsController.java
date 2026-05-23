package growzapp.backend.module.projet.controller;

import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.investissement.service.InvestissementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard-stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Dashboard", description = "Indicateurs clés de performance (KPI) pour le tableau de bord administrateur")
public class AdminStatsController {

    private final InvestissementService investissementService;
    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final InvestissementRepository investissementRepository;

    @GetMapping
    @Operation(
        summary = "Données du tableau de bord administrateur",
        description = "Retourne les KPI financiers et opérationnels de la plateforme : montant total collecté, objectif global, nombre d'investissements, nombre d'utilisateurs, évolution mensuelle et répartition par secteur.",
        tags = {"Admin - Dashboard"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Données du dashboard retournées",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class,
                    example = "{\"success\": true, \"message\": \"Opération réussie\", \"data\": {" +
                        "\"totalCollecte\": 125000.00, " +
                        "\"totalObjectif\": 500000.00, " +
                        "\"countInvestissements\": 48, " +
                        "\"countUsers\": 120, " +
                        "\"evolution\": [], " +
                        "\"secteurs\": {\"Agriculture\": 3, \"Énergie\": 2}" +
                        "}}"))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        List<Projet> tousLesProjets = projetService.getAllAdmin(null);

        BigDecimal totalCollecte = tousLesProjets.stream()
                .map(Projet::getMontantCollecte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalObjectif = tousLesProjets.stream()
                .map(Projet::getObjectifFinancement)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("totalCollecte", totalCollecte);
        data.put("totalObjectif", totalObjectif);
        data.put("countInvestissements", investissementRepository.count());
        data.put("countUsers", userRepository.count());
        data.put("evolution", investissementService.getInvestmentEvolution());
        data.put("secteurs", tousLesProjets.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSecteur() != null ? p.getSecteur().getNom() : "Inconnu",
                        Collectors.counting())));

        return ApiResponseDTO.success(data);
    }
}
