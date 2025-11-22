// src/main/java/growzapp/backend/controller/web/ProfileController.java
package growzapp.backend.controller.web;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.service.DividendeService;
import growzapp.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/profile") // ← TOUS LES ENDPOINTS SOUS /profile
@RequiredArgsConstructor
public class ProfileController {

        private final ProjetRepository projetRepository;
        private final InvestissementRepository investissementRepository;
        private final DividendeService dividendeService;
        private final UserService userService;

        // === DASHBOARD PROFIL ===
        @GetMapping
        public String dashboard(Model model) {
                User user = userService.getCurrentUser();

                if (user == null) {
                        return "redirect:/projets"; // ou /login si tu veux forcer
                }

                model.addAttribute("user", user);

                // === RÔLES ===
                boolean estPorteur = user.getRoles().stream()
                                .anyMatch(r -> "PORTEUR".equals(r.getRole()));
                boolean estInvestisseur = user.getRoles().stream()
                                .anyMatch(r -> "INVESTISSEUR".equals(r.getRole()));

                model.addAttribute("estPorteur", estPorteur);
                model.addAttribute("estInvestisseur", estInvestisseur);

                // === PORTEUR ===
                if (estPorteur) {
                        long nbProjets = projetRepository.countByPorteurId(user.getId());
                        model.addAttribute("nbProjets", nbProjets);
                }

                // === INVESTISSEUR ===
                if (estInvestisseur) {
                        long nbInvestissements = investissementRepository.countByInvestisseurId(user.getId());
                        model.addAttribute("nbInvestissements", nbInvestissements);

                        double totalPercu = dividendeService.getTotalPercuPayeByInvestisseur(user.getId());
                        model.addAttribute("totalPercu", totalPercu);
                }

                return "profile/dashboard";
        }

        // === MES PROJETS ===
        @GetMapping("/mes-projets")
        public String mesProjets(Model model) {
                User user = userService.getCurrentUser();

                if (user == null) {
                        return "redirect:/profile"; // reste dans l’espace privé
                }

                model.addAttribute("projets", projetRepository.findByPorteurId(user.getId()));
                return "profile/mes-projets";
        }

        // === MES INVESTISSEMENTS ===
        @GetMapping("/mes-investissements")
        public String mesInvestissements(Model model) {
                User user = userService.getCurrentUser();

                if (user == null) {
                        return "redirect:/profile";
                }

                // Récupère avec dividendes
                List<Investissement> investissements = investissementRepository
                                .findByInvestisseurIdWithDividendes(user.getId());

                model.addAttribute("investissements",
                                investissements != null ? investissements : Collections.emptyList());

                // Charge les factures (lazy)
                if (investissements != null) {
                        investissements.forEach(inv -> inv.getDividendes()
                                        .forEach(div -> Hibernate.initialize(div.getFacture())));
                }

                double totalPercu = dividendeService.getTotalPercuPayeByInvestisseur(user.getId());
                model.addAttribute("totalPercu", totalPercu);

                return "profile/mes-investissements";
        }
}