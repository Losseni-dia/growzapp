package growzapp.backend.module.projet.controller;

import growzapp.backend.module.dividende.enums.StatutDividende;
import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.dividende.repository.DividendeRepository;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.projet.dto.AdminGlobalDashboardDTO;
import growzapp.backend.module.projet.dto.GlobalKpiDTO;
import growzapp.backend.module.projet.dto.TopProjetDTO;
import growzapp.backend.module.projet.dto.VelocitePointDTO;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.user.repository.UserRepository;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/v1/admin/dashboard-stats", "/api/admin/dashboard-stats"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Dashboard", description = "Indicateurs clés de performance (KPI) pour le tableau de bord administrateur")
public class AdminStatsController {

    private final InvestissementService investissementService;
    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final InvestissementRepository investissementRepository;
    private final DividendeRepository dividendeRepository;
    private final ProjetTraductionRepository traductionRepository;

    private static final DateTimeFormatter MOIS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @GetMapping
    @Operation(summary = "Données du tableau de bord administrateur (legacy)", description = "Retourne les KPI financiers et opérationnels de la plateforme : montant total collecté, objectif global, nombre d'investissements, nombre d'utilisateurs, évolution mensuelle et répartition par secteur.", tags = {
            "Admin - Dashboard" })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Données du dashboard retournées", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis", content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
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

    // ═══════════════════════════════════════════════════════════════════
    // ── NOUVEAU DASHBOARD GLOBAL — restructuré, façon bourse ────────────
    // ═══════════════════════════════════════════════════════════════════
    @GetMapping("/global")
    @Operation(summary = "Tableau de bord global admin (restructuré)", description = "Vue plateforme complète : investissements, projets, dividendes — organisée en sections distinctes.", tags = {
            "Admin - Dashboard" })
    public ApiResponseDTO<AdminGlobalDashboardDTO> getGlobalDashboard(
            @RequestParam(required = false, defaultValue = "fr") String langue) {

        List<Projet> tousLesProjets = projetService.getAllAdmin(null);

        List<Investissement> tousLesInvestissementsValides = investissementRepository.findAll().stream()
                .filter(i -> i.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                .toList();

        // ── KPIs GLOBAUX ─────────────────────────────────────────────────
        BigDecimal totalCollecte = tousLesProjets.stream()
                .map(Projet::getMontantCollecte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalObjectif = tousLesProjets.stream()
                .map(Projet::getObjectifFinancement)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double tauxCompletionGlobal = totalObjectif.compareTo(BigDecimal.ZERO) > 0
                ? totalCollecte.divide(totalObjectif, MathContext.DECIMAL128)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue()
                : 0.0;

        long countProjetsActifs = tousLesProjets.stream()
                .filter(p -> p.getStatutProjet() != null && "VALIDE".equals(p.getStatutProjet().name()))
                .count();

        long countInvestisseursUniques = tousLesInvestissementsValides.stream()
                .map(i -> i.getInvestisseur() != null ? i.getInvestisseur().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        BigDecimal montantTotalInvesti = tousLesInvestissementsValides.stream()
                .map(Investissement::getMontantInvesti)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montantMoyenInvestissement = !tousLesInvestissementsValides.isEmpty()
                ? montantTotalInvesti.divide(BigDecimal.valueOf(tousLesInvestissementsValides.size()),
                        MathContext.DECIMAL128)
                : BigDecimal.ZERO;

        GlobalKpiDTO kpis = new GlobalKpiDTO(
                totalCollecte,
                totalObjectif,
                tauxCompletionGlobal,
                userRepository.count(),
                countInvestisseursUniques,
                countProjetsActifs,
                tousLesInvestissementsValides.size(),
                montantMoyenInvestissement);

        // ── ÉVOLUTION MENSUELLE DES INVESTISSEMENTS ─────────────────────
        Map<String, List<Investissement>> investissementsParMois = tousLesInvestissementsValides.stream()
                .filter(i -> i.getDate() != null)
                .collect(Collectors.groupingBy(i -> i.getDate().format(MOIS_FORMAT)));

        List<VelocitePointDTO> evolutionInvestissements = investissementsParMois.entrySet().stream()
                .map(entry -> {
                    BigDecimal montantMois = entry.getValue().stream()
                            .map(Investissement::getMontantInvesti)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new VelocitePointDTO(entry.getKey(), montantMois, entry.getValue().size());
                })
                .sorted(Comparator.comparing(VelocitePointDTO::periode))
                .toList();

        // ── RÉPARTITIONS PROJETS ────────────────────────────────────────
        Map<String, Long> repartitionParSecteur = tousLesProjets.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getSecteur() != null ? p.getSecteur().getNom() : "Inconnu",
                        Collectors.counting()));

        Map<String, Long> repartitionParStatut = tousLesProjets.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getStatutProjet() != null ? p.getStatutProjet().name() : "INCONNU",
                        Collectors.counting()));

        // ── TOP PROJETS ──────────────────────────────────────────────────
        List<TopProjetDTO> topParCollecte = tousLesProjets.stream()
                .filter(p -> p.getMontantCollecte() != null)
                .sorted(Comparator.comparing(Projet::getMontantCollecte).reversed())
                .limit(5)
                .map(p -> toTopProjetDTO(p, langue))
                .toList();

        List<TopProjetDTO> topParProgression = tousLesProjets.stream()
                .filter(p -> p.getObjectifFinancement() != null
                        && p.getObjectifFinancement().compareTo(BigDecimal.ZERO) > 0
                        && p.getMontantCollecte() != null)
                .sorted((a, b) -> Double.compare(
                        b.getMontantCollecte().divide(b.getObjectifFinancement(), MathContext.DECIMAL128)
                                .doubleValue(),
                        a.getMontantCollecte().divide(a.getObjectifFinancement(), MathContext.DECIMAL128)
                                .doubleValue()))
                .limit(5)
                .map(p -> toTopProjetDTO(p, langue))
                .toList();

        // ── DIVIDENDES (agrégats uniquement, plateforme entière) ────────
        List<Dividende> dividendesPayes = dividendeRepository.findAll().stream()
                .filter(d -> d.getStatutDividende() == StatutDividende.PAYE)
                .toList();

        BigDecimal totalDividendesVerses = dividendesPayes.stream()
                .map(Dividende::getMontantTotal)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, List<Dividende>> dividendesParMois = dividendesPayes.stream()
                .filter(d -> d.getDatePaiement() != null)
                .collect(Collectors.groupingBy(d -> d.getDatePaiement().format(MOIS_FORMAT)));

        List<VelocitePointDTO> evolutionDividendes = dividendesParMois.entrySet().stream()
                .map(entry -> {
                    BigDecimal montantMois = entry.getValue().stream()
                            .map(Dividende::getMontantTotal)
                            .filter(Objects::nonNull)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new VelocitePointDTO(entry.getKey(), montantMois, entry.getValue().size());
                })
                .sorted(Comparator.comparing(VelocitePointDTO::periode))
                .toList();

        AdminGlobalDashboardDTO dashboard = new AdminGlobalDashboardDTO(
                kpis,
                evolutionInvestissements,
                repartitionParSecteur,
                repartitionParStatut,
                topParCollecte,
                topParProgression,
                totalDividendesVerses,
                dividendesPayes.size(),
                evolutionDividendes);

        return ApiResponseDTO.success(dashboard);
    }

    private TopProjetDTO toTopProjetDTO(Projet p, String langue) {
        double progression = p.getObjectifFinancement() != null
                && p.getObjectifFinancement().compareTo(BigDecimal.ZERO) > 0
                        ? p.getMontantCollecte().divide(p.getObjectifFinancement(), MathContext.DECIMAL128)
                                .multiply(BigDecimal.valueOf(100))
                                .doubleValue()
                        : 0.0;

        String libelleTradu = null;
        if (langue != null && !langue.isBlank() && !langue.equals("fr")) {
            Optional<ProjetTraductionProjection> traduction = traductionRepository
                    .findProjectionByProjetIdAndLangue(p.getId(), langue);
            if (traduction.isPresent() && traduction.get().getLibelle() != null) {
                libelleTradu = traduction.get().getLibelle();
            }
        }

        return new TopProjetDTO(
                p.getId(),
                p.getLibelle(),
                libelleTradu,
                p.getPoster(),
                p.getMontantCollecte(),
                p.getObjectifFinancement(),
                progression);
    }
}