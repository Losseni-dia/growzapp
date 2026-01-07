package growzapp.backend.controller.api.admin;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.InvestissementService;
import growzapp.backend.service.ProjetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard-stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final InvestissementService investissementService;
    private final ProjetService projetService;
    private final UserRepository userRepository;
    private final ProjetRepository projetRepository;
    private final InvestissementRepository investissementRepository;

    @GetMapping
    public ApiResponseDTO<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // 1. KPI Financiers Globaux
        BigDecimal totalCollecte = projetRepository.findAll().stream()
                .map(Projet::getMontantCollecte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalObjectif = projetRepository.findAll().stream()
                .map(Projet::getObjectifFinancement)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        data.put("totalCollecte", totalCollecte);
        data.put("totalObjectif", totalObjectif);
        data.put("countInvestissements", investissementRepository.count());
        data.put("countUsers", userRepository.count());

        // 2. Données pour la Courbe d'évolution (Montants investis par mois)
        data.put("evolution", investissementService.getInvestmentEvolution());

        // 3. Répartition par secteur (Nombre de projets)
        data.put("secteurs", projetService.getAll().stream()
                .collect(Collectors.groupingBy(
                        p -> p.secteurNom() != null ? p.secteurNom() : "Inconnu",
                        Collectors.counting())));

        return ApiResponseDTO.success(data);
    }
}