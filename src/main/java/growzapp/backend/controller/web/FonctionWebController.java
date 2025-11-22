package growzapp.backend.controller.web;

import growzapp.backend.model.entite.Fonction;
import growzapp.backend.service.FonctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/fonctions")
@RequiredArgsConstructor
public class FonctionWebController {

    private final FonctionService fonctionService;

    // === LISTE ===
    @GetMapping
    public String listFonctions(Model model) {
        List<Fonction> fonctions = fonctionService.getAll();
        model.addAttribute("fonctions", fonctions);
        model.addAttribute("title", "Liste des fonctions");
        model.addAttribute("module", "fonctions");
        return "fonction/index"; // → templates/fonction/index.html
    }

    // === CRÉER (formulaire) ===
    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("fonction", new Fonction());
        model.addAttribute("title", "Créer une fonction");
        return "fonction/form";
    }

    // === CRÉER (soumission) ===
        @PostMapping("/create")
    public String create(@Valid @ModelAttribute Fonction fonction, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("title", "Créer une fonction");
            return "fonction/form";
        }
        
        Fonction saved = fonctionService.save(fonction);
        
        // Message de succès
        model.addAttribute("success", 
            saved.getId() == null ? 
            "Fonction '" + saved.getNom() + "' existe déjà." : 
            "Fonction '" + saved.getNom() + "' créée avec succès."
        );
        
        return "redirect:/fonctions";
    }

    // === ÉDITER (formulaire) ===
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Fonction fonction = fonctionService.getById(id);
        model.addAttribute("fonction", fonction);
        model.addAttribute("title", "Modifier " + fonction.getNom());
        return "fonction/form";
    }

    // === ÉDITER (soumission) ===
    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute Fonction fonction) {
        fonction.setId(id);
        fonctionService.save(fonction);
        return "redirect:/fonctions";
    }

    // === SUPPRIMER ===
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        fonctionService.deleteById(id);
        return "redirect:/fonctions";
    }
}