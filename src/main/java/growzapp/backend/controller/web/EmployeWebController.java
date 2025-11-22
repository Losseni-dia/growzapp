package growzapp.backend.controller.web;

import growzapp.backend.model.entite.Employe;
import growzapp.backend.model.entite.EmployeFonction;
import growzapp.backend.service.EmployeService;
import growzapp.backend.service.FonctionService;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employes")
@RequiredArgsConstructor
public class EmployeWebController {

    private final EmployeService employeService;
    private final FonctionService fonctionService;

    // === LISTE ===
    @GetMapping
    public String listEmployes(Model model) {
        List<Employe> employes = employeService.getAll();
        model.addAttribute("employes", employes);
        model.addAttribute("title", "Liste des employés");
        model.addAttribute("module", "employes");
        return "employe/index";
    }

    // === CRÉER (formulaire) ===
   @GetMapping("/create")
    public String createForm(Model model) {
        Employe employe = new Employe();
        employe.getFonctions().add(new EmployeFonction()); // 1 ligne vide
        model.addAttribute("employe", employe);
        model.addAttribute("title", "Créer un employé");
        return "employe/form";
    }
    // === ÉDITER (formulaire) ===
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Employe employe = employeService.getById(id);
        model.addAttribute("employe", employe);
        model.addAttribute("title", "Modifier " + employe.getPrenom());
        model.addAttribute("module", "employes");
        return "employe/form";
    }

    // === SAUVEGARDER (CREATE + UPDATE) ===
    @PostMapping(value = { "/create", "/{id}/edit" })
    public String saveEmploye(@PathVariable(required = false) Long id,
            @ModelAttribute Employe employe) {
        if (id != null) {
            employe.setId(id);
        }
        employeService.save(employe);
        return "redirect:/employes";
    }

    // === SUPPRIMER ===
    @PostMapping("/{id}/delete")
    public String deleteEmploye(@PathVariable Long id) {
        employeService.deleteById(id);
        return "redirect:/employes";
    }

        // === AJOUTER FONCTION À L'EMPLOYÉ ===
    @GetMapping("/{id}")
    public String showEmploye(@PathVariable Long id, Model model) {
        Employe employe = employeService.getById(id);
        model.addAttribute("employe", employe);
        model.addAttribute("allFonctions", fonctionService.getAll()); // ← Liste des fonctions
        model.addAttribute("title", employe.getPrenom() + " " + employe.getNom());
        return "employe/show";
    }

    @PostMapping("/{id}/add-fonction")
    public String addFonctionToEmploye(
            @PathVariable Long id,
            @RequestParam Long fonctionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate datePriseFonction) {

        employeService.addFonctionToEmploye(id, fonctionId, datePriseFonction);
        return "redirect:/employes/" + id;
    }
}