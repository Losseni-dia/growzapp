// src/main/java/growzapp/backend/controller/web/AdminWebController.java
package growzapp.backend.controller.web;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.localisationDTO.LocalisationDTO;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Role;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.StatutDividende;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.RoleRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminWebController {

        private final ProjetService projetService;
        private final UserService userService;
        private final EmployeService employeService;
        private final LocaliteService localiteService;
        private final LocalisationService localisationService;
        private final DividendeService dividendeService;
        private final InvestissementService investissementService;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;

        // === DASHBOARD ADMIN ===
        @GetMapping
        public String dashboard(Model model, HttpServletRequest request) {
                model.addAttribute("currentPath", request.getRequestURI());

                // === PROJETS ===
                List<ProjetDTO> projets = projetService.getAll();
                long totalProjets = projets.size();
                long enCours = countByStatut(projets, StatutProjet.EN_COURS);
                long termines = countByStatut(projets, StatutProjet.TERMINE);
                // Somme des montants collectés
                BigDecimal montantCollecteTotal = projets.stream()
                        .map(ProjetDTO::montantCollecte)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Somme des objectifs de financement
                BigDecimal objectifTotal = projets.stream()
                        .map(ProjetDTO::objectifFinancement)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                double tauxRemplissage = objectifTotal.compareTo(BigDecimal.ZERO) > 0 ? (montantCollecteTotal.divide(objectifTotal, 2, RoundingMode.HALF_UP).doubleValue()) * 100 : 0;

                // === INVESTISSEMENTS ===
                List<InvestissementDTO> investissements = investissementService.getAllInvestissements();
                long totalInvestissements = investissements.size();
                BigDecimal montantInvestiTotal = investissements.stream()
                                .map(inv -> BigDecimal.valueOf(inv.nombrePartsPris()).multiply(inv.prixUnePart()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                long totalPartsVendues = sumLong(investissements, InvestissementDTO::nombrePartsPris);

                double roiMoyen = projets.stream()
                                .mapToDouble(ProjetDTO::roiProjete)
                                .filter(roi -> roi != 0)
                                .average()
                                .orElse(0.0);

                // === UTILISATEURS ===
               // List<UserDTO> users = userService.getAll();
               // long totalUsers = users.size();
                //long investisseurs = users.stream().filter(u -> !u.getInvestissements().isEmpty()).count();
                //long porteurs = users.stream().filter(u -> !u.getProjets().isEmpty()).count();

                // === EMPLOYÉS ===
                long totalEmployes = employeService.count();

                // === LOCALISATIONS ===
                List<LocaliteDTO> localites = localiteService.getAll();
                long totalLocalites = localites.size();
                Map<String, Long> localitesParPays = localites.stream()
                                .collect(Collectors.groupingBy(
                                                l -> l.paysNom() != null ? l.paysNom() : "Inconnu",
                                                Collectors.counting()));

                List<LocalisationDTO> sites = localisationService.getAll();
                long totalSites = sites.size();

                // === SITES PAR PAYS ===
                Map<String, Long> sitesParPays = sites.stream()
                                .filter(s -> s.paysNom() != null)
                                .collect(Collectors.groupingBy(
                                                LocalisationDTO::paysNom,
                                                Collectors.counting()));
        // === DIVIDENDES ===
        List<DividendeDTO> dividendes = dividendeService.getAll();
        long totalDividendes = dividendes.size();
        long verses = countByStatut(dividendes, StatutDividende.PAYE);
        long planifies = countByStatut(dividendes, StatutDividende.PLANIFIE);

        // Somme des montants versés (seulement les dividendes PAYE)
        BigDecimal montantVerseTotal = dividendes.stream()
                .filter(d -> d.statutDividende() == StatutDividende.PAYE)
                .map(DividendeDTO::montantTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Taux de distribution : (montant versé / montant investi total) * 100, arrondi à 2 décimales
        BigDecimal tauxDistribution = BigDecimal.ZERO;

        if (montantInvestiTotal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal montantInvestiBig = montantInvestiTotal;
            BigDecimal ratio = montantVerseTotal.divide(montantInvestiBig, 10, RoundingMode.HALF_UP);
            tauxDistribution = ratio.multiply(BigDecimal.valueOf(100)); // en pourcentage
            tauxDistribution = tauxDistribution.setScale(2, RoundingMode.HALF_UP); // 2 décimales
        }

        // === AJOUT AU MODÈLE ===
        model.addAttribute("totalProjets", totalProjets);
        model.addAttribute("enCours", enCours);
        model.addAttribute("termines", termines);
        model.addAttribute("montantCollecteTotal", montantCollecteTotal);
        model.addAttribute("objectifTotal", objectifTotal);
        model.addAttribute("tauxRemplissage", Math.round(tauxRemplissage * 100.0) / 100.0); // 2 décimales

        model.addAttribute("totalInvestissements", totalInvestissements);
        model.addAttribute("montantInvestiTotal", montantInvestiTotal);
        model.addAttribute("totalPartsVendues", totalPartsVendues);
        model.addAttribute("roiMoyen", String.format("%.1f", roiMoyen));

        model.addAttribute("totalEmployes", totalEmployes);

        model.addAttribute("totalLocalites", totalLocalites);
        model.addAttribute("localitesParPays", localitesParPays);
        model.addAttribute("totalSites", totalSites);
        model.addAttribute("sitesParPays", sitesParPays);

        model.addAttribute("totalDividendes", totalDividendes);
        model.addAttribute("verses", verses);
        model.addAttribute("planifies", planifies);
        model.addAttribute("montantVerseTotal", montantVerseTotal);

        // Affichage du taux en pourcentage avec 2 décimales (ex: 45.67)
        model.addAttribute("tauxDistribution", tauxDistribution.doubleValue());

        model.addAttribute("title", "Tableau de bord Admin");

        return "admin/dashboard";
        }

        // === LISTE DES UTILISATEURS ===
        @GetMapping("/users")
        public String listeUsers(Model model, HttpServletRequest request) {
                model.addAttribute("currentPath", request.getRequestURI());
               // List<UserDTO> users = userService.getAll();
               // model.addAttribute("users", users);
                model.addAttribute("title", "Liste des Utilisateurs");
                return "admin/users";
        }

        // === TOUS LES INVESTISSEMENTS ===
        @GetMapping("/investissements")
        public String listeInvestissements(Model model, HttpServletRequest request) {
                model.addAttribute("currentPath", request.getRequestURI());
                List<InvestissementDTO> investissements = investissementService.getAllInvestissements();
                model.addAttribute("investissements", investissements);
                model.addAttribute("title", "Tous les Investissements");
                return "admin/investissements";
        }

        // === LISTE DES DIVIDENDES ===
        @GetMapping("/dividendes")
        public String listeDividendes(Model model, HttpServletRequest request) {
                model.addAttribute("currentPath", request.getRequestURI());
                List<DividendeDTO> dividendes = dividendeService.getAll();
                model.addAttribute("dividendes", dividendes);
                model.addAttribute("title", "Liste des Dividendes");
                return "admin/dividendes";
        }

       // === MÉTHODES UTILITAIRES (SÉCURISÉES) ===
private <T> long countByStatut(List<T> list, Enum<?> statut) {
    if (statut == null || list == null) return 0;
    return list.stream()
               .map(this::getStatut)
               .filter(Objects::nonNull)
               .filter(s -> s.equals(statut))
               .count();
}

private <T> double sum(List<T> list, ToDoubleFunction<? super T> mapper) {
    if (list == null) return 0.0;
    return list.stream()
               .mapToDouble(item -> {
                   try {
                       return mapper.applyAsDouble(item);
                   } catch (Exception e) {
                       return 0.0;
                   }
               })
               .sum();
}

private <T> long sumLong(List<T> list, ToLongFunction<? super T> mapper) {
    if (list == null) return 0L;
    return list.stream()
               .mapToLong(item -> {
                   try {
                       return mapper.applyAsLong(item);
                   } catch (Exception e) {
                       return 0L;
                   }
               })
               .sum();
}

private <T> Enum<?> getStatut(T item) {
    if (item instanceof ProjetDTO p && p.statutProjet() != null) {
        return p.statutProjet();
    }
    if (item instanceof DividendeDTO d && d.statutDividende() != null) {
        return d.statutDividende();
    }
    return null;
}

//Changer les roles
@PostMapping("/admin/users/{userId}/roles")
@Transactional
public ResponseEntity<ApiResponseDTO<Void>> updateUserRoles(
        @PathVariable Long userId,
        @RequestBody Map<String, List<String>> body) {

    List<String> requestedRoles = body.get("roles");
    if (requestedRoles == null)
        requestedRoles = List.of();

    Set<String> validRoles = Set.of("ADMIN", "PORTEUR", "INVESTISSEUR");
    if (!validRoles.containsAll(requestedRoles)) {
        return ResponseEntity.badRequest()
                .body(ApiResponseDTO.error("Rôle invalide"));
    }

    try {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Set<Role> newRoles = requestedRoles.stream()
                .map(name -> roleRepository.findByRole(name)
                        .orElseThrow(() -> new RuntimeException("Rôle non trouvé: " + name)))
                .collect(Collectors.toSet());

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
        userRepository.saveAndFlush(user);

        String message = newRoles.isEmpty()
                ? "Tous les rôles ont été retirés"
                : "Rôles mis à jour avec succès";

        return ResponseEntity.ok(
                ApiResponseDTO.<Void>success(null).message(message));

    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(500)
                .body(ApiResponseDTO.error(e.getMessage()));
    }
}


}