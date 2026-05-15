package growzapp.backend.controller.api.admin.projet;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.service.ProjetService;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.InvestissementService;
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
public class AdminStatsController {

        private final InvestissementService investissementService;
        private final ProjetService projetService;
        private final UserRepository userRepository;
        private final InvestissementRepository investissementRepository;

        @GetMapping
        public ApiResponseDTO<Map<String, Object>> getDashboardData() {
                Map<String, Object> data = new HashMap<>();

                // 1. Récupération des entités
                List<Projet> tousLesProjets = projetService.getAllAdmin(null);

                // 2. KPI Financiers (Calculés sur les entités)
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

                // 3. Évolution (Service métier)
                data.put("evolution", investissementService.getInvestmentEvolution());

                // 4. Répartition par secteur (Calculée sur les entités)
                data.put("secteurs", tousLesProjets.stream()
                                .collect(Collectors.groupingBy(
                                                p -> p.getSecteur() != null ? p.getSecteur().getNom() : "Inconnu",
                                                Collectors.counting())));

                return ApiResponseDTO.success(data);
        }
}