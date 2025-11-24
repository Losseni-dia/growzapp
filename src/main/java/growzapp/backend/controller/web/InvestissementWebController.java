// src/main/java/growzapp/backend/controller/web/InvestissementWebController.java
package growzapp.backend.controller.web;

import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.service.InvestissementService;
import growzapp.backend.service.ProjetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/investissements")
@RequiredArgsConstructor
public class InvestissementWebController {

    private final InvestissementRepository investissementRepository;
    private final InvestissementService investissementService;
    private final ProjetService projetService;

    // === LISTE GÉNÉRALE (admin ou debug) ===
   

    // === MES INVESTISSEMENTS (connecté) ===
    @GetMapping("/mes-investissements")
    public String mesInvestissements(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("investissements",
                investissementRepository.findByInvestisseurId(user.getId()));
        return "investissement/mes-investissements";
    }

    // === FORMULAIRE CRÉATION (connecté obligatoire) ===
    @GetMapping("/create")
    public String createForm(@AuthenticationPrincipal User user, Model model) {
       /* List<ProjetDTO> projets = projetService.getProjetsDisponibles();*/

        InvestissementDTO dto = InvestissementDTO.builder()
                .nombrePartsPris(1)
                .date(LocalDateTime.now())
                .frais(0.0)
                .statutPartInvestissement(StatutPartInvestissement.EN_ATTENTE)
                .investisseurId(user.getId())
                .investisseurNom(user.getPrenom() + " " + user.getNom())
                .build();

        /*model.addAttribute("projets", projets); */
        model.addAttribute("investissement", dto);
        model.addAttribute("title", "Nouveau Investissement");
        return "investissement/form";
    }

    // === FORMULAIRE ÉDITION (seulement ses investissements) ===
   /*  @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {
        InvestissementDTO dto = investissementService.getInvestissementDtoById(id);

        // Sécurité : l'utilisateur ne peut éditer que ses propres investissements
        if (!dto.investisseurId().equals(user.getId())) {
            return "redirect:/investissements/mes-investissements?error=unauthorized";
        }

        List<ProjetDTO> projets = projetService.getProjetsDisponibles();
        model.addAttribute("investissement", dto);
        model.addAttribute("projets", projets);
        model.addAttribute("title", "Modifier Investissement");
        return "investissement/form";
    }

    // === SAUVEGARDE (création ou édition) ===
    @PostMapping(value = { "/create", "/edit/{id}" })
    public String save(
            @PathVariable(required = false) Long id,
            @Valid @ModelAttribute("investissement") InvestissementDTO dto,
            BindingResult result,
            @AuthenticationPrincipal User user,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("projets", projetService.getProjetsDisponibles());
            return "investissement/form";
        }

        // Sécurité : forcer l'investisseur = utilisateur connecté
        dto = dto.toBuilder()
                .investisseurId(user.getId())
                .investisseurNom(user.getPrenom() + " " + user.getNom())
                .build();

        //investissementService.save(dto, null);
        return "redirect:/investissements/mes-investissements";
    } */

    // === ADMIN : LISTE COMPLÈTE ===
    @GetMapping("/admin")
    public String listeAdmin(Model model) {
        model.addAttribute("investissements", investissementRepository.findAll());
        return "investissement/liste-admin";
    }

    // === ADMIN : VALIDER ===
    @PostMapping("/{id}/valider")
    public String validerInvestissement(@PathVariable Long id, RedirectAttributes ra) {
        try {
            investissementService.validerInvestissement(id);
            ra.addFlashAttribute("success", "Investissement validé ! Contrat envoyé.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/investissements/admin";
    }

    // === ADMIN : ANNULER ===
    @PostMapping("/{id}/annuler")
    public String annulerInvestissement(@PathVariable Long id, RedirectAttributes ra) {
        try {
            investissementService.annulerInvestissement(id);
            ra.addFlashAttribute("success", "Investissement annulé.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Erreur : " + e.getMessage());
        }
        return "redirect:/investissements/admin";
    }

    // === DÉTAIL INVESTISSEMENT ===
   // @GetMapping("/{id}")
   /*  public String showInvestissement(@PathVariable Long id, Model model) {
        Investissement investissement = investissementService.findById(id);
        investissement.getDividendes().forEach(div -> Hibernate.initialize(div.getFacture()));
        model.addAttribute("investissement", investissement);
        return "investissement/show";
    } */

    // === DIVIDENDES D’UN INVESTISSEMENT ===
    @GetMapping("/{id}/dividendes")
    public String showDividendes(@PathVariable Long id, Model model) {
        InvestissementDTO investissement = investissementService.getInvestissementWithAllDividendes(id);
        model.addAttribute("investissement", investissement);
        return "investissement/mes-dividendes";
    }

    // === TEST EMAIL (désactiver en prod) ===
    @PostMapping("/test/investissement")
    public String testInvestissement() throws Exception {
        // ... ton code de test
        return "Email envoyé ! Vérifie ta boîte.";
    }
}